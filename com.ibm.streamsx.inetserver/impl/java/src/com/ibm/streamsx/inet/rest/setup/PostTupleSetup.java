/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2014 
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
import com.ibm.streamsx.inet.rest.servlets.InjectForm;
import com.ibm.streamsx.inet.rest.servlets.InjectTuple;

/**
 * Sets up the single servlet for Tuple injection.
 */
public class PostTupleSetup implements OperatorServletSetup {

	/**
	 * Servlet that injects tuples based upon a application/x-www-form-urlencoded POST
	 * Servlet that provides a basic HTML form for tuple injection.
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

			String path = "/output/" + port.getPortNumber() + "/inject";
			ports.addServlet(new ServletHolder(new InjectTuple(operatorContext, port)), path);

			ep.addURL("inject", path);
			trace.info("Injection URL (application/x-www-form-urlencoded): " + ports.getContextPath() + path);

			path = "/output/" + port.getPortNumber() + "/form";
			ports.addServlet(new ServletHolder(new InjectForm(port)), path);
			ep.addURL("form", path);
			trace.info("Injection FORM URL: " + ports.getContextPath() + path);
		}

		return exposed;
	}
}
