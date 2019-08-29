/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019, 2020
*/
package com.ibm.streamsx.inet.rest.setup;

import java.util.List;

import org.eclipse.jetty.servlet.ServletContextHandler;

import com.ibm.streamsx.inet.rest.ops.ServletOperator;

/**
 * Does not setup any operator specific servlets.
 *
 */
public class WebContextSetup implements OperatorServletSetup {

	@Override
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports) {
		return null;
	}
}
