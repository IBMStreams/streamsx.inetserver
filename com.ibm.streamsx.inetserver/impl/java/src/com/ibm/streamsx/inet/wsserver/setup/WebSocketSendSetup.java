package com.ibm.streamsx.inet.wsserver.setup;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streamsx.inet.rest.setup.ExposedPort;
import com.ibm.streamsx.inet.rest.setup.OperatorServletSetup;
import com.ibm.streamsx.inet.wsserver.servlet.WebSocketSendServlet;

public class WebSocketSendSetup implements OperatorServletSetup {

	/**
	 * Servlet that registers the Event Socket to the jetty server
	 * @return 
	 */
	@Override
	public List<ExposedPort> setup(OperatorContext operatorContext, ServletContextHandler handler, ServletContextHandler ports) {
		
		Logger trace = Logger.getAnonymousLogger();
		
		List<ExposedPort> exposed = new ArrayList<ExposedPort>();

		for (StreamingInput<Tuple> port : operatorContext.getStreamingInputs()) {

			String path = "/input/" + port.getPortNumber() + "/wssend";
			ServletHolder servletHolder = new ServletHolder(new WebSocketSendServlet(operatorContext));
			ports.addServlet(servletHolder, path);

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);
			ep.addURL("/wssend", path);

			trace.info("wssend URL: " + ports.getContextPath() + path);
		}

		return exposed;
	}

}
