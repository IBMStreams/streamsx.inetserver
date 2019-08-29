package com.ibm.streamsx.inet.wsserver.setup;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.rest.setup.ExposedPort;
import com.ibm.streamsx.inet.rest.setup.OperatorServletSetup;
import com.ibm.streamsx.inet.wsserver.servlet.WebSocketInjectServlet;

public class WebSocketInjectSetup implements OperatorServletSetup {

	/**
	 * Servlet that registers the Event Socket to the jetty server
	 * @return 
	 */
	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {
		
		Logger trace = Logger.getAnonymousLogger();
		
		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		OperatorContext operatorContext = operator.getOperatorContext();

		for (StreamingOutput<OutputTuple> port : operatorContext.getStreamingOutputs()) {

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);

			String path = "/output/" + port.getPortNumber() + "/wsinject";
			ServletHolder servletHolder = new ServletHolder(new WebSocketInjectServlet(operatorContext));
			ports.addServlet(servletHolder, path);

			ep.addURL("/wsinject", path);

			trace.info("wsinject URL: " + ports.getContextPath() + path);
		}

		return exposed;
	}

}
