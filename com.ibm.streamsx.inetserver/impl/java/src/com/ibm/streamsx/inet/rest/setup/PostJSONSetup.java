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
import com.ibm.streamsx.inet.rest.servlets.InjectJSON;

/**
 * Sets up the single servlet for JSON injection.
 */
public class PostJSONSetup implements OperatorServletSetup {

	/**
	 * Servlet that accepts application/json POST and submits a
	 * corresponding tuple with the first attribute being an XML attribute.
	 * @return 
	 */
	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {

		Logger trace = Logger.getLogger(PostJSONSetup.class.getName());
		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		OperatorContext operatorContext = operator.getOperatorContext();

		for (StreamingOutput<OutputTuple> port : operatorContext.getStreamingOutputs()) {
			
			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);

			String path = "/output/" + port.getPortNumber() + "/inject";
			ports.addServlet(new ServletHolder(new InjectJSON(operatorContext, port)), path);
			ep.addURL("inject", path);

			trace.info("Injection URL (application/json): " + ports.getContextPath() + path);
		}

		return exposed;
	}
}
