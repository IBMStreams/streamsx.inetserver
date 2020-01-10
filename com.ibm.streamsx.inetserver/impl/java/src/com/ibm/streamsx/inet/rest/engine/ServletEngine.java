/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019, 2020  
*/
package com.ibm.streamsx.inet.rest.engine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64.Decoder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.relation.MBeanServerNotificationFilter;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.ProcessingElement;
import com.ibm.streams.operator.StreamingData;
import com.ibm.streams.operator.management.OperatorManagement;
import com.ibm.streamsx.inet.messages.Messages;
import com.ibm.streamsx.inet.rest.ops.Functions;
import com.ibm.streamsx.inet.rest.ops.PostTuple;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.rest.servlets.ExposedContextInfo;
import com.ibm.streamsx.inet.rest.servlets.ExposedPortsInfo;
import com.ibm.streamsx.inet.rest.servlets.PortInfo;
import com.ibm.streamsx.inet.rest.setup.ExposedPort;
import com.ibm.streamsx.inet.rest.setup.OperatorServletSetup;
import com.ibm.streamsx.inet.util.PathConversionHelper;

/**
 * Eclipse Jetty Servlet engine that can be shared by multiple operators
 * within the same PE. Sharing is performed via JMX to
 * avoid class loading issues due to each operator having
 * its own classloader and hence its own version of the jetty
 * libraries.
 * Supports multiple servlet engines within the same PE,
 * one per defined port.
 */
public class ServletEngine implements ServletEngineMBean, MBeanRegistration {

	static Logger trace = Logger.getLogger(ServletEngine.class.getName());

	private static final Object syncMe = new Object();

	public static final int DEFAULT_PORT = 8080;
	public static final String CONTEXT_RESOURCE_BASE_PARAM = "contextResourceBase";
	public static final String CONTEXT_PARAM = "context";

	public static final String SSL_CERT_ALIAS_PARAM = "certificateAlias";
	public static final String SSL_KEYSTORE_PARAM = "keyStore";
	public static final String SSL_KEYSTORE_PASSWORD_PARAM = "keyStorePassword";
	public static final String SSL_KEY_PASSWORD_PARAM = "keyPassword";

	public static final String SSL_TRUSTSTORE_PARAM = "trustStore";
	public static final String SSL_TRUSTSTORE_PASSWORD_PARAM = "trustStorePassword";
	
	public static final String SSL_APP_CONFIG_NAME_PARAM = "sslAppConfigName";

	public static final int IDLE_TIMEOUT = 30000;
	public static final int STRICT_TRANSPORT_SECURITY_MAX_AGE = 2000;
	
	public static final String METRIC_NAME_HTTPS = "https";
	public static final String METRIC_NAME_PORT = "serverPort";

	public static ServletEngineMBean getServletEngine(OperatorContext operatorContext) throws Exception {

		int portNumber = DEFAULT_PORT;
		if (operatorContext.getParameterNames().contains("port"))
			portNumber = Integer.valueOf(operatorContext.getParameterValues("port").get(0));

		String host = null;
		if (operatorContext.getParameterNames().contains("host"))
			host = operatorContext.getParameterValues("host").get(0);

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		final ObjectName jetty = new ObjectName("com.ibm.streamsx.inet.rest:type=jetty,port=" + portNumber);
		synchronized (syncMe) {
			if (!mbs.isRegistered(jetty)) {
				try {
					mbs.registerMBean(new ServletEngine(jetty, operatorContext, portNumber, host), jetty);
				} catch (InstanceAlreadyExistsException infe) {
				}
			}
		}
		
		return JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), jetty, ServletEngineMBean.class);
	}

	private final OperatorContext startingOperatorContext;

	private boolean started;
	private boolean stopped;

	private final Server server;
	private final ObjectName myObjectName;
	private boolean isSSL;
	// Jetty port.
	private int localPort;
	private final ContextHandlerCollection handlers;
	private final Map<String, ServletContextHandler> staticContexts = Collections.synchronizedMap(new HashMap<String, ServletContextHandler>());
	private final List<ExposedPort> exposedPorts = Collections.synchronizedList(new ArrayList<ExposedPort>());

	private ServletEngine(ObjectName myObjectName, OperatorContext operatorContext, int portNumber, String host) throws Exception {
		this.myObjectName = myObjectName;
		this.startingOperatorContext = operatorContext;
		final ThreadPoolExecutor tpe = newContextThreadPoolExecutor(operatorContext);

		ThreadPool tp = new ThreadPool() {
			@Override
			public void execute(Runnable runnable) {
				try {
					tpe.execute(runnable);
				} catch (RejectedExecutionException e) {
				}
			}
			@Override
			public int getIdleThreads() {
				return tpe.getPoolSize() - tpe.getActiveCount();
			}
			@Override
			public int getThreads() {
				return tpe.getPoolSize();
			}
			@Override
			public boolean isLowOnThreads() {
				return tpe.getActiveCount() >= tpe.getMaximumPoolSize();
			}
			@Override
			public void join() throws InterruptedException {
				while (true) {
					Thread.sleep(600L * 1000L);
				}
			}
		};

		server = new Server(tp);
		handlers = new ContextHandlerCollection();

		if (	   (operatorContext.getParameterNames().contains(SSL_CERT_ALIAS_PARAM))
				|| (operatorContext.getParameterNames().contains(SSL_APP_CONFIG_NAME_PARAM)))
			setHTTPSConnector(operatorContext, server, portNumber, host);
		else
			setHTTPConnector(operatorContext, server, portNumber, host);
		operatorContext.getMetrics().getCustomMetric(METRIC_NAME_HTTPS).setValue(isSSL ? 1 : 0);

		ServletContextHandler portsIntro = new ServletContextHandler(server, "/ports", ServletContextHandler.SESSIONS);
		portsIntro.addServlet(new ServletHolder( new ExposedPortsInfo(exposedPorts)), "/info");
		addHandler(portsIntro);

		ServletContextHandler contextIntro = new ServletContextHandler(server, "/contexts", ServletContextHandler.SESSIONS);
		contextIntro.addServlet(new ServletHolder( new ExposedContextInfo(staticContexts)), "/info");
		addHandler(contextIntro);

		// making a abs path by combining toolkit directory with the opt/resources dir
		URI baseToolkitDir = operatorContext.getToolkitDirectory().toURI();
		addStaticContext("streamsx.inet.resources", PathConversionHelper.convertToAbsPath(baseToolkitDir, "opt/resources"));

		String streamsInstall = System.getenv("STREAMS_INSTALL");
		if (streamsInstall != null) {
			File dojo = new File(streamsInstall, "ext/dojo");
			addStaticContext("streamsx.inet.dojo", dojo.getAbsolutePath());
		}
	}

	/**
	 * Setup an HTTP connector.
	 */
	private void setHTTPConnector(OperatorContext operatorContext, Server server, int portNumber, String host) {
		HttpConfiguration http_config = new HttpConfiguration();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(http_config));
		connector.setPort(portNumber);
		if (host != null)
			connector.setHost(host);
		connector.setIdleTimeout(IDLE_TIMEOUT);
		server.addConnector(connector);
	}

	/**
	 * Setup an HTTPS connector.
	 * @throws KeyStoreException 
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 */
	private void setHTTPSConnector(OperatorContext operatorContext, Server server, int httpsPort, String host)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

		//ssl configuration with legacy parameters
		if (operatorContext.getParameterNames().contains(SSL_CERT_ALIAS_PARAM)) {
			
			//Key Store is required
			String keyStorePath = operatorContext.getParameterValues(SSL_KEYSTORE_PARAM).get(0);
			File keyStorePathFile = new File(keyStorePath);
			if (!keyStorePathFile.isAbsolute())
				keyStorePathFile = new File(operatorContext.getPE().getApplicationDirectory(), keyStorePath);
			String keyStorePathToLoad = keyStorePathFile.getAbsolutePath();
			System.out.println("keyStorePathToLoad=" + keyStorePathToLoad);
			sslContextFactory.setKeyStorePath(keyStorePathToLoad);
			
			//the key store password is optional
			if (operatorContext.getParameterNames().contains(SSL_KEYSTORE_PASSWORD_PARAM)) {
				String keyStorePassword = operatorContext.getParameterValues(SSL_KEYSTORE_PASSWORD_PARAM).get(0);
				System.out.println("keyStorePassword=****");
				sslContextFactory.setKeyStorePassword(Functions.obfuscate(keyStorePassword));
			}
			
			//Key password is required
			String keyPassword = operatorContext.getParameterValues(SSL_KEY_PASSWORD_PARAM).get(0);
			System.out.println("keyPassword=****");
			sslContextFactory.setKeyManagerPassword(Functions.obfuscate(keyPassword));
			
			//Key alias
			String alias = operatorContext.getParameterValues(SSL_CERT_ALIAS_PARAM).get(0);
			sslContextFactory.setCertAlias(alias);
			
			//Trust Store & password if necessary
			if (operatorContext.getParameterNames().contains(SSL_TRUSTSTORE_PARAM)) {
				String trustStorePath = operatorContext.getParameterValues(SSL_TRUSTSTORE_PARAM).get(0);
				File trustStorePathFile = new File(trustStorePath);
				if (!trustStorePathFile.isAbsolute())
					trustStorePathFile = new File(operatorContext.getPE().getApplicationDirectory(), trustStorePath);
				String trustStorePathToLoad = trustStorePathFile.getAbsolutePath();
				System.out.println("trustStorePathToLoad=" + trustStorePathToLoad);
				sslContextFactory.setTrustStorePath(trustStorePathToLoad);
				sslContextFactory.setNeedClientAuth(true);
				if (operatorContext.getParameterNames().contains(SSL_TRUSTSTORE_PASSWORD_PARAM)) {
					String trustStorePassword = operatorContext.getParameterValues(SSL_TRUSTSTORE_PASSWORD_PARAM).get(0);
					System.out.println("trustStorePassword=****");
					sslContextFactory.setTrustStorePassword(Functions.obfuscate(trustStorePassword));
				}
			}

		//Configuration with application configuration
		} else {
			
			ProcessingElement pe = operatorContext.getPE();
			String certAppConfigName = operatorContext.getParameterValues(SSL_APP_CONFIG_NAME_PARAM).get(0);
			Map<String, String> certProps = pe.getApplicationConfiguration(certAppConfigName);
			System.out.println("streams-certs len: " + new Integer(certProps.size()).toString() + " keyset: " + certProps.keySet().toString());
			if (certProps.isEmpty())
				throw new IllegalArgumentException(Messages.getString("APP_CONFIG_REQURED", certAppConfigName));
			
			//Key Store and password is required
			if ( ! certProps.containsKey("server.jks"))
				throw new IllegalArgumentException(Messages.getString("APP_CONFIG_PROP_REQURED", "server.jks", certAppConfigName));
			String keyB64Str = certProps.get("server.jks");
			Decoder decoder = Base64.getDecoder();
			byte[] keyBytes = decoder.decode(keyB64Str);
			InputStream keyStream = new ByteArrayInputStream(keyBytes);

			if ( ! certProps.containsKey("server.pass"))
				throw new IllegalArgumentException(Messages.getString("APP_CONFIG_PROP_REQURED", "server.pass", certAppConfigName));
			String password = certProps.get("server.pass");

			System.out.println("Load key store and passwd from app config " + certAppConfigName);
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(keyStream, password.toCharArray());
			sslContextFactory.setKeyStore(keyStore);
			sslContextFactory.setKeyManagerPassword(password);
			
			//Set optionally trust material
			if (certProps.containsKey("cacerts.jks")) {
				String trustB64Str = certProps.get("cacerts.jks");
				byte[] trustBytes = decoder.decode(trustB64Str);
				InputStream trustStream = new ByteArrayInputStream(trustBytes);
				
				System.out.println("Load trust store and passwd from app config " + certAppConfigName);
				KeyStore trustStore = KeyStore.getInstance("JKS");
				trustStore.load(trustStream, password.toCharArray());
				sslContextFactory.setTrustStore(trustStore);
				sslContextFactory.setNeedClientAuth(true);
			}
		}

		sslContextFactory.setRenegotiationAllowed(false);
		sslContextFactory.setIncludeProtocols("TLSv1.2");
		String[] specs = {"^.*_(MD5|SHA|SHA1)$","^TLS_RSA_.*$","^.*_NULL_.*$","^.*_anon_.*$"};
		sslContextFactory.setExcludeCipherSuites(specs);

		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		http_config.setSecurePort(httpsPort);
		HttpConfiguration https_config = new HttpConfiguration(http_config);
		SecureRequestCustomizer src = new SecureRequestCustomizer();
		src.setStsMaxAge(STRICT_TRANSPORT_SECURITY_MAX_AGE);
		src.setStsIncludeSubDomains(true);
		https_config.addCustomizer(src);

		ServerConnector connector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(https_config));

		connector.setPort(httpsPort);
		if (host != null)
			connector.setHost(host);
		connector.setIdleTimeout(IDLE_TIMEOUT);
		server.addConnector(connector); 

		isSSL = true;
	}


	// Originally corePoolSize was set to a fixed: 32
	// Jetty, however, creates its starting threads based on the number of
	// available processors 2*(Runtime.getRuntime().availableProcessors()+3)/4
	// On large hosts (ppc64 with 24 processors, this can exceed 32)
	// While many descriptions of the ThreadPoolExecuter make it seem that it will
	// just add threads, testing has shown that this did not occur.
	// Some literature states it will only add threads if the queue is full
	// If Jetty never starts, then the queue will never fill, thus
	// we need core threads to be set to at least as large as the number of threads
	// that Jetty will start
	// NOTE: This was based on examination of jetty 8.1.3 code
	//       If the toolkit moves to jetty 9+ this could change
	private ThreadPoolExecutor newContextThreadPoolExecutor(OperatorContext operatorContext) {
		int jettyStartupThreads = 2*(Runtime.getRuntime().availableProcessors()+3)/4;
		trace.info("Creating ThreadPoolExecuter corePoolSize: 32+" + jettyStartupThreads);
		return  new ThreadPoolExecutor(
				32 + jettyStartupThreads, // corePoolSize,
				Math.max(256, 32 + jettyStartupThreads), // maximumPoolSize,
				60, //keepAliveTime,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), // workQueue,
				operatorContext.getThreadFactory());
	}

	private synchronized void addHandler(ServletContextHandler newHandler) {
		handlers.addHandler(newHandler);
		handlers.mapContexts();
	}

	@Override
	public void start() throws Exception {
		synchronized (this) {
			if (started) {
				return;
			}
			started = true;
		}
		startWebServer();
	}

	@Override
	public void stop() throws Exception {
		synchronized (this) {
			if (stopped || !started) {
				return;
			}
			stopped = true;
			notifyAll();
		}
		stopWebServer();
	}

	/**
	 * Add a default servlet that allows an operator
	 * to pull static resources from a single location.
	 * Typically used with getThisFileDir(). 
	 * @throws Exception 
	 */
	private ServletContextHandler addOperatorStaticContext(OperatorContext operatorContext) throws Exception {

		if (!operatorContext.getParameterNames().contains(CONTEXT_PARAM))
			return null;
		if (!operatorContext.getParameterNames().contains(CONTEXT_RESOURCE_BASE_PARAM))
			return null;

		String ctxName = operatorContext.getParameterValues(CONTEXT_PARAM).get(0);
		String resourceBase = operatorContext.getParameterValues(CONTEXT_RESOURCE_BASE_PARAM).get(0);

		if ("".equals(ctxName))
			throw new IllegalArgumentException(Messages.getString("PARAM_MUST_NOT_BE_EMPTY", CONTEXT_PARAM));

		if ("".equals(resourceBase))
			throw new IllegalArgumentException(Messages.getString("PARAM_MUST_NOT_BE_EMPTY", CONTEXT_RESOURCE_BASE_PARAM));

		// Convert resourceBase file path to absPath if it is relative, if relative, it should be relative to application directory.
		URI baseConfigURI = operatorContext.getPE().getApplicationDirectory().toURI();
		return addStaticContext(ctxName, PathConversionHelper.convertToAbsPath(baseConfigURI, resourceBase));
	}

	private ServletContextHandler addStaticContext(String ctxName, String resourceBase) throws Exception {
		
		if (staticContexts.containsKey(ctxName))
			return staticContexts.get(ctxName);
		
		ServletContextHandler cntx = new ServletContextHandler(server, "/" + ctxName, ServletContextHandler.SESSIONS);
		
		cntx.setWelcomeFiles(new String[] { "index.html" });
		cntx.setResourceBase(resourceBase);
		
		ResourceHandler rh = new ResourceHandler();
		rh.setDirectoriesListed(true);
		cntx.setHandler(rh);

		addHandler(cntx);
		staticContexts.put(ctxName, cntx);
		
		trace.info("Static context: " + cntx.getContextPath() + " resource base: " + resourceBase);

		return cntx;
	}

    private void startWebServer() throws Exception {

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setWelcomeFiles(new String[] { "index.html" });

        URI baseResourceURI = startingOperatorContext.getPE().getApplicationDirectory().toURI();

        resource_handler.setResourceBase(PathConversionHelper.convertToAbsPath(baseResourceURI, "opt/html"));
        trace.info("Common context: index.html resource base: " + resource_handler.getResourceBase());

        HandlerList topLevelhandlers = new HandlerList();
        topLevelhandlers.setHandlers(new Handler[] { handlers, resource_handler, new DefaultHandler() });

        server.setHandler(topLevelhandlers);
        server.start();
        Thread t = startingOperatorContext.getThreadFactory().newThread(new Runnable() {
            public void run() {

                try {
                    server.join();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        });

        t.setDaemon(false);
        t.start();

        ServerConnector sc = (ServerConnector)server.getConnectors()[0];
        localPort = sc.getLocalPort();
        startingOperatorContext.getMetrics().getCustomMetric("serverPort").setValue(localPort);
    }


	private void stopWebServer() throws Exception {
		trace.info("Stop Jetty web server");
		server.stop();
	}

	@Override
	public void registerOperator(ServletOperator operator, Object conduit) throws Exception {

		final OperatorContext operatorContext = operator.getOperatorContext();
		trace.info("Register servlets for operator: " + operatorContext.getName());

		final ServletContextHandler staticContext = addOperatorStaticContext(operatorContext);
		if (staticContext != null) {
			staticContext.setAttribute("operator.context", operatorContext);
			if (conduit != null)
				staticContext.setAttribute("operator.conduit", conduit);
		}

		// If there is a context parameter in this operator
		// just use the base name of the operator (without the composite nesting qualifiers)
		// as the lead in for port resources exposed by this operator.
		// Otherwise use the full operator name so that it is unique.
		String ctxName = null;
		if (operatorContext.getParameterNames().contains(CONTEXT_PARAM)) {
			ctxName = operatorContext.getParameterValues(CONTEXT_PARAM).get(0);

			if ("".equals(ctxName))
				throw new IllegalArgumentException(Messages.getString("PARAM_MUST_NOT_BE_EMPTY", CONTEXT_PARAM));

		}
		
		String leadIn = operatorContext.getName(); // .replace('.', '/');
		if (ctxName != null && leadIn.indexOf('.') != -1) {
			leadIn = leadIn.substring(leadIn.lastIndexOf('.') + 1);
		}

		// Standard ports context for URLs relative to ports.
		ServletContextHandler ports = null;
		if (operatorContext.getNumberOfStreamingInputs() != 0 ||
				operatorContext.getNumberOfStreamingOutputs() != 0) {

			String portsContextPath = "/" + leadIn + "/ports";
			if (ctxName != null)
				portsContextPath = "/" + ctxName + portsContextPath;
			ports = new ServletContextHandler(server, portsContextPath, ServletContextHandler.SESSIONS);

			ports.setAttribute("operator.context", operatorContext);
			if (conduit != null)
				ports.setAttribute("operator.conduit", conduit);

			trace.info("Ports context: " + ports.getContextPath());

			if (operatorContext.getParameterNames().contains(PostTuple.MAX_CONTENT_SIZE_PARAM)) {
				int maxContentSize = Integer.parseInt(operatorContext.getParameterValues(PostTuple.MAX_CONTENT_SIZE_PARAM).get(0)) * 1000;
				if (maxContentSize > 0) {
					trace.info("Maximum content size for context: " + ports.getContextPath() + " increased to " + maxContentSize);
					ports.setMaxFormContentSize(maxContentSize);
				}
			}
		}

		// Automatically add info servlet for all output and input ports
		for (StreamingData port : operatorContext.getStreamingOutputs()) {
			String path = "/output/" + port.getPortNumber() + "/info";
			ports.addServlet(new ServletHolder(new PortInfo(operatorContext, port)),  path);
			trace.info("Port information servlet URL : " + ports.getContextPath() + path);
		}
		for (StreamingData port : operatorContext.getStreamingInputs()) {
			String path = "/input/" + port.getPortNumber() + "/info";
			ports.addServlet(new ServletHolder(new PortInfo(operatorContext, port)),  path);
			trace.info("Port information servlet URL : " + ports.getContextPath() + path);
		}

		// Add servlets for the operator, driven by a Setup class that implements OperatorServletSetup.
		final String operatorSetupClass = operator.getSetupClass();
		OperatorServletSetup setup = Class.forName(operatorSetupClass).asSubclass(OperatorServletSetup.class).newInstance();
		
		List<ExposedPort> operatorPorts = setup.setup(operator, staticContext, ports);
		if (operatorPorts != null)
			exposedPorts.addAll(operatorPorts);
		
		if (ports != null)
			addHandler(ports);
	}

	/*public static class OperatorWebAppContext extends WebAppContext {
		public OperatorWebAppContext() {
		}
	}*/

	@Override
	public void postDeregister() {
	}

    /**
     * On PE shutdown unregister this MBean, allows unit tests
     * to have multiple executions in the same JVM.
     */
    @Override
    public void postRegister(Boolean registrationDone) {
        
        MBeanServerNotificationFilter unregisterPe = new MBeanServerNotificationFilter();
        unregisterPe.disableAllTypes();
        unregisterPe.disableAllTypes();
        unregisterPe.enableObjectName(OperatorManagement.getPEName());
        unregisterPe.enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
        
        try {
            ManagementFactory.getPlatformMBeanServer().addNotificationListener(
                    MBeanServerDelegate.DELEGATE_NAME, new NotificationListener() {
                        
                        @Override
                        public void handleNotification(Notification notification, Object handback) {
                            try {
                                ManagementFactory.getPlatformMBeanServer().unregisterMBean(myObjectName);
                            } catch (MBeanRegistrationException e) {
                                ;
                            } catch (InstanceNotFoundException e) {
                                ;
                            }
                        }
                    }, unregisterPe, null);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preDeregister() throws Exception {
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        return null;
    }
}
