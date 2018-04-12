//
// *******************************************************************************
// * Copyright (C)2018, International Business Machines Corporation and *
// * others. All Rights Reserved. *
// *******************************************************************************
//
package com.ibm.streamsx.inet.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.logging.TraceLevel;
import com.ibm.streamsx.inet.messages.Messages;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

/**
 * Handles the HTTP client & authentication for the HTTPRequest operator.
 * TODO: All client support, just uses a default client atm.
 */
class HTTPRequestOperClient extends HTTPRequestOperAPI {
    
    protected Properties props = null;
    protected CloseableHttpClient httpClient = null;
    protected CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    protected HttpClientContext httpContext = HttpClientContext.create();
    protected OAuthConsumer oAuthConsumer = null;
    protected String oAuth2AuthHeaderKey = null;
    protected String oAuth2AuthHeaderValue = null;

    
    /******************************************************************
     * initialize
     * @throws Exception 
     ******************************************************************/
    @Override
    public void initialize(OperatorContext context) throws Exception {
        super.initialize(context);
        
        //Authentication parameters
        initializeAuthProperties(context);
        initializeOauth1();
        
        buildHttpClient();
    }
    
    /*
     * Authentication parameters to http context
     */
    private void initializeAuthProperties(OperatorContext context) throws FileNotFoundException, IOException {
        //Authentication parameters
        boolean hasAuthenticationFile = (authenticationFile != null && ! authenticationFile.isEmpty());
        boolean hasAuthenticationProperties = (authenticationProperties != null && ! authenticationProperties.isEmpty());
        if (hasAuthenticationFile || hasAuthenticationProperties) {
            props = new Properties();
            //read property file
            if (hasAuthenticationFile) {
                tracer.log(TraceLevel.DEBUG, "AuthenticationFile=" + authenticationFile);
                props.load(new FileReader(authenticationFile));
            }
            //override with values from authenticationProperties parameter
            if (hasAuthenticationProperties) {
                for(String line : authenticationProperties) {
                    int loc = line.indexOf("=");
                    if(loc == -1) 
                        throw new IllegalArgumentException(Messages.getString("PROP_ILLEGAL_AUTH_PROP", line));
                    String name = line.substring(0, loc);
                    String val  = line.substring(loc+1, line.length());
                    props.setProperty(name, val);
                }
            }
            //set credentials if auth type is STANDARD
            if (authenticationType.equals(AuthenticationType.STANDARD)) {
                Set<String> propNames = props.stringPropertyNames();
                ArrayList<AuthScope> allScopes = new ArrayList<>();
                for (String authScope : propNames) {
                    AuthScope as = AuthScope.ANY;
                    if ( (! authScope.equals("ANY")) && (! authScope.equals("ANY_HOST"))) {
                        as = new AuthScope(authScope, AuthScope.ANY_PORT);
                    }
                    allScopes.add(as);
                    String value = props.getProperty(authScope);
                    tracer.log(TraceLevel.TRACE, "AuthProp " + authScope + "=" + value);
                    int loc = value.indexOf(":");
                    if (loc == -1)
                        throw new IllegalArgumentException(Messages.getString("PROP_INVALID_VALUE", authScope, value));
                    String user = value.substring(0, loc);
                    String pass = value.substring(loc+1, value.length());
                    credentialsProvider.setCredentials(as, new UsernamePasswordCredentials(user, pass));
                }
                //some tracing
                StringBuilder sb = new StringBuilder("Start with credentials\n");
                for (AuthScope ac : allScopes) {
                    sb.append(credentialsProvider.getCredentials(ac));
                    sb.append("\n");
                }
                tracer.log(TraceLevel.DEBUG, sb.toString());
                //add credentials to http context
                httpContext.setCredentialsProvider(credentialsProvider);
            }
        }
    }
    
    /*
     * Build http client dependent on ssl context
     */
    private void buildHttpClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException {
        //ssl 
        SSLContext sslDefaultContext = SSLContext.getDefault();
        String[] sslDefaultProtocols = sslDefaultContext.getDefaultSSLParameters().getProtocols();

        if (tracer.isLoggable(TraceLevel.TRACE)) {
            String msg = "sslDefaultProtocols : ";
            for (int i=0; i<sslDefaultProtocols.length; i++) {
                Integer itg = new Integer(i);
                msg = msg + "sslDefaultProtocols[" + itg.toString() + "]=" + sslDefaultProtocols[i] + " ";
            }
            tracer.log(TraceLevel.TRACE, msg);
        }

        HttpClientBuilder clientBuilder = HttpClients.custom();

        //trust all certificates
        if (sslAcceptAllCertificates) {
            tracer.log(TraceLevel.DEBUG, "sslAcceptAllCertificates=" + sslAcceptAllCertificates);
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            SSLContext sslContext = sslContextBuilder.build();
            
            sslContext.init(
                null,
                new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
                },
                new SecureRandom()
            );

            SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(
                    sslContext,
                    sslDefaultProtocols,
                    null,
                    NoopHostnameVerifier.INSTANCE
            );
            
            clientBuilder.setSSLSocketFactory(sslcsf);
            //clientBuilder.useSystemProperties();
        }

        // Trust own CA and all self-signed certs
        if (sslTrustStoreFile != null) {
            tracer.log(TraceLevel.DEBUG, "sslTrustStoreFile=" + sslTrustStoreFile);
            SSLContext sslcontext = SSLContexts.custom()
                .loadTrustMaterial(new File(sslTrustStoreFile), sslTrustStorePassword.toCharArray(), new TrustSelfSignedStrategy())
                .build();
            
            SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    sslDefaultProtocols,
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            );
            
            clientBuilder.setSSLSocketFactory(sslcsf);
        }
        httpClient = clientBuilder.build();
    }

    /*
     * Initialize oauth context
     */
    private void initializeOauth1() {
        switch (authenticationType) {
        case OAUTH1:
            oAuthConsumer = new CommonsHttpOAuthConsumer(getRequiredProperty("consumerKey"), getRequiredProperty("consumerSecret"));
            oAuthConsumer.setTokenWithSecret(getRequiredProperty("accessToken"), getRequiredProperty("accessTokenSecret"));
            break;
        case OAUTH2:
            oAuth2AuthHeaderKey = new String("Authorization");
            oAuth2AuthHeaderValue = new String(props.getProperty("authMethod", "Bearer") + " " + getRequiredProperty("accessToken"));
            break;
        case STANDARD:
            break;
        }
    }

    /**
     * Returns the property value. 
     * @param name
     * @throws RuntimeException if a property is not found.
     * @return
     */
    protected String getRequiredProperty(String name) {
        if(props == null)
            throw new RuntimeException(Messages.getString("PROP_REQUIRED_1", name));
        String ret = props.getProperty(name);
        if(ret == null)
            throw new RuntimeException(Messages.getString("PROP_REQUIRED_2", name));
        return ret;
    }

}
