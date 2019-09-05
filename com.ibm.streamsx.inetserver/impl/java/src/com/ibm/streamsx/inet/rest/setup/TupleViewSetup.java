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
import com.ibm.streams.operator.window.StreamWindow;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.rest.ops.TupleView;
import com.ibm.streamsx.inet.rest.servlets.AccessWindowContents;
import com.ibm.streamsx.inet.window.WindowContentsAtTrigger;


public class TupleViewSetup implements OperatorServletSetup {

	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {

		List<ExposedPort> exposed = new ArrayList<ExposedPort>();
		TupleView tupleViewOperator = (TupleView) operator;
		OperatorContext operatorContext = operator.getOperatorContext();

		final List<WindowContentsAtTrigger<Tuple>> windows = new ArrayList<WindowContentsAtTrigger<Tuple>>(operatorContext.getNumberOfStreamingOutputs());

		Logger trace = Logger.getLogger(TupleViewSetup.class.getName());

		List<StreamingInput<Tuple>> inputPorts = operatorContext.getStreamingInputs();
		for (int i = 0; i < inputPorts.size(); i++) {
			
			StreamingInput<Tuple> port = inputPorts.get(i);
			StreamWindow<Tuple> window = port.getStreamWindow();

			WindowContentsAtTrigger<Tuple> contents = new WindowContentsAtTrigger<Tuple>(tupleViewOperator, i);
			windows.add(contents);
			window.registerListener(contents, false);

			String path = "/input/" + port.getPortNumber() + "/tuples";
			ports.addServlet(new ServletHolder(new AccessWindowContents(contents)),  path);

			ExposedPort ep = new ExposedPort(operatorContext, port, ports.getContextPath());
			exposed.add(ep);
			ep.addURL("tuples", path);

			trace.info("Port JSON URL: " + ports.getContextPath() + path);
		}
		return exposed;
	}
}
