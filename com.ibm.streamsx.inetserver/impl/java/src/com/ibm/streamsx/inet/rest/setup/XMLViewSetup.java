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
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.rest.servlets.AccessXMLAttribute;

public class XMLViewSetup implements OperatorServletSetup {

	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {

		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		OperatorContext operatorContext = operator.getOperatorContext();

		Logger trace = Logger.getLogger(XMLViewSetup.class.getName());

		// The XMLView operator only supports a single port
		// at the moment, but code the ability to have multiple ports.
		for (StreamingInput<Tuple> port : operatorContext.getStreamingInputs()) {
			String path = "/input/" + port.getPortNumber() + "/attribute";
			ports.addServlet(new ServletHolder(new AccessXMLAttribute(port)),  path);

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);
			ep.addURL("attribute", path);

			trace.info("Port XML Attribute URL: " + ports.getContextPath() + path);
		}
		return exposed;
	}
}
