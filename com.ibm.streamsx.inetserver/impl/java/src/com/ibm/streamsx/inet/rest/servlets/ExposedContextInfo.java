/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019, 2020
*/
package com.ibm.streamsx.inet.rest.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletContextHandler;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

@SuppressWarnings("serial")
public class ExposedContextInfo extends HttpServlet {
	
	private final Map<String, ServletContextHandler> staticContexts;
	
	public ExposedContextInfo(Map<String, ServletContextHandler> staticContexts) {
		this.staticContexts = staticContexts;
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		response.setStatus(HttpServletResponse.SC_OK);

		JSONObject jresp = new JSONObject();
		JSONArray jcontexts = new JSONArray();
		for (String ctx : staticContexts.keySet()) {
			jcontexts.add("/" + ctx);
		}
		jresp.put("contexts", jcontexts);
		out.println(jresp.serialize());
		out.flush();
		out.close();
	}
}
