/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019, 2020
*/
package com.ibm.streamsx.inet.rest.setup;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.rest.servlets.InjectWithResponse;

/**
 * Sets up the single servlet for HTTPRequestProcess.
 */
public class RequestProcessSetup implements OperatorServletSetup {

	/**
	 * Servlet that injects a tuple at request with function from conduit
	 * @return 
	 */
	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {
		
		Logger trace = Logger.getLogger(RequestProcessSetup.class.getName());
		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		OperatorContext operatorContext = operator.getOperatorContext();

		for (StreamingOutput<OutputTuple> port : operatorContext.getStreamingOutputs()) {

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);

			String path = "/analyze/" + port.getPortNumber() + "/*";
			ServletHolder servletHolder = new ServletHolder(new InjectWithResponse(operatorContext, port));
			ports.addServlet(servletHolder, path);
			servletHolder.setAsyncSupported(true);
			
			ep.addURL("analyze", path);

			trace.info("Analyze URL: " + ports.getContextPath() + path);
		}

		return exposed;
	}
}
