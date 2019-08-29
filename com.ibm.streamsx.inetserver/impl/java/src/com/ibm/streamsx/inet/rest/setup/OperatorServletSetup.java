/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2014 
*/
package com.ibm.streamsx.inet.rest.setup;

import java.util.List;

import org.eclipse.jetty.servlet.ServletContextHandler;

import com.ibm.streamsx.inet.rest.ops.ServletOperator;

/**
 * Interface to set up the servlets for an operator using Jetty.
 * 
 * operator = Operator reference 
 * staticContext = the jetty ServletContextHandler for the static content
 * ports = the jetty ServletContextHandler for the static content
 * 
 * These attributes are available using the init method of the servlet,
 * see AccessXMLAttribute for an example.
 */
public interface OperatorServletSetup {
	
	public List<ExposedPort> setup(ServletOperator operator, ServletContextHandler staticContext, ServletContextHandler ports);
}
