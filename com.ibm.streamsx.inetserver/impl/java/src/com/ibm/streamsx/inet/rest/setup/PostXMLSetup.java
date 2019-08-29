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
import com.ibm.streamsx.inet.rest.servlets.InjectXML;

/**
 * Sets up the single servlet for XML injection.
 */
public class PostXMLSetup implements OperatorServletSetup {

	/**
	 * Servlet that accepts application/xml POST and submits a
	 * corresponding tuple with the first attribute being an XML attribute.
	 * @return 
	 */
	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {

		Logger trace = Logger.getLogger(PostXMLSetup.class.getName());
		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		OperatorContext operatorContext = operator.getOperatorContext();

		for (StreamingOutput<OutputTuple> port : operatorContext.getStreamingOutputs()) {

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);

			String path = "/output/" + port.getPortNumber() + "/inject";
			ports.addServlet(new ServletHolder(new InjectXML(operatorContext, port)), path);
			ep.addURL("inject", path);

			trace.info("Injection URL (application/xml): " + ports.getContextPath() + path);
		}

		return exposed;
	}
}
