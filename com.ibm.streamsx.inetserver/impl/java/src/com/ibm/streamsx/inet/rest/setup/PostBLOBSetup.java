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
import com.ibm.streamsx.inet.rest.servlets.InjectBLOB;

/**
 * Sets up the single servlet for BLOB injection.
 */
public class PostBLOBSetup implements OperatorServletSetup {

	/**
	 * Servlet that accepts application/octet-stream POST and submits a
	 * corresponding tuple with the first attribute being a blob attribute.
	 * @return 
	 */
	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {

		Logger trace = Logger.getLogger(PostBLOBSetup.class.getName());
		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		OperatorContext operatorContext = operator.getOperatorContext();

		for (StreamingOutput<OutputTuple> port : operatorContext.getStreamingOutputs()) {

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);

			String path = "/output/" + port.getPortNumber() + "/inject";
			ports.addServlet(new ServletHolder(new InjectBLOB(port)), path);
			ep.addURL("inject", path);

			trace.info("Injection URL (application/octet-stream): " + ports.getContextPath() + path);
		}

		return exposed;
	}
}
