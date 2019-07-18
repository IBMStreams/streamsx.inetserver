/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2014 
*/
package com.ibm.streamsx.inet.rest.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import com.ibm.streams.flow.declare.OperatorGraph;
import com.ibm.streams.flow.declare.OperatorGraphFactory;
import com.ibm.streams.flow.declare.OperatorInvocation;
import com.ibm.streams.flow.javaprimitives.JavaOperatorTester;
import com.ibm.streams.flow.javaprimitives.JavaTestableGraph;
import com.ibm.streamsx.inet.rest.ops.PostBLOB;
import com.ibm.streamsx.inet.rest.ops.PostJSON;
import com.ibm.streamsx.inet.rest.ops.PostTuple;
import com.ibm.streamsx.inet.rest.ops.PostXML;
import com.ibm.streamsx.inet.rest.ops.RequestProcess;
import com.ibm.streamsx.inet.rest.ops.TupleView;
import com.ibm.streamsx.inet.rest.ops.WebContext;
import com.ibm.streamsx.inet.rest.ops.XMLView;
import com.ibm.streamsx.inet.wsserver.ops.WebSocketInject;
import com.ibm.streamsx.inet.wsserver.ops.WebSocketSend;

public class ContextTest {
	
	@Test
	public void testContext() throws Exception {
	
		File cf1 = File.createTempFile("webc", ".txt");
		File cf2 = File.createTempFile("webc", ".txt");
		cf2.delete();
		
		assertEquals(cf1.getParentFile(), cf2.getParentFile());

		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare all inetserver operators
		OperatorInvocation<WebContext> webContextOp = graph.addOperator(WebContext.class);
		webContextOp.setStringParameter("context", "cwc").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		
		OperatorInvocation<TupleView> tupleViewOp = graph.addOperator(TupleView.class);
		tupleViewOp.setStringParameter("context", "ctv").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		
		OperatorInvocation<XMLView> xmlViewOp = graph.addOperator(XMLView.class);
		xmlViewOp.setStringParameter("context", "cxv").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		xmlViewOp.addInput("tuple<xml value>");
		
		OperatorInvocation<PostTuple> postTupleOp = graph.addOperator(PostTuple.class);
		postTupleOp.setStringParameter("context", "cpt").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		
		OperatorInvocation<PostXML> postXMLOp = graph.addOperator(PostXML.class);
		postXMLOp.setStringParameter("context", "cpx").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		
		OperatorInvocation<PostJSON> postJSONOp = graph.addOperator(PostJSON.class);
		postJSONOp.setStringParameter("context", "cpj").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		
		OperatorInvocation<PostBLOB> postBLOBOp = graph.addOperator(PostBLOB.class);
		postBLOBOp.setStringParameter("context", "cpb").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		
		OperatorInvocation<WebSocketInject> webSocketInjectOp = graph.addOperator(WebSocketInject.class);
		webSocketInjectOp.setStringParameter("context", "cwi").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		webSocketInjectOp.addOutput("tuple<rstring data>");
		
		OperatorInvocation<WebSocketSend> webSocketSendOp = graph.addOperator(WebSocketSend.class);
		webSocketSendOp.setStringParameter("context", "cws").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);

		OperatorInvocation<RequestProcess> requestProcessOp = graph.addOperator(RequestProcess.class);
		requestProcessOp.setStringParameter("context", "crp").setStringParameter("contextResourceBase", cf1.getParent()).setIntParameter("port", 0);
		requestProcessOp.addOutput("tuple<int64 key, rstring request, rstring contentType, rstring response, rstring method, rstring pathInfo, int32 status>");
		requestProcessOp.addInput("tuple<int64 key>");
		
		// Create the testable version of the graph
		JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);

		// Execute the initialization of operators within graph.
		testableGraph.initialize().get().allPortsReady().get();
		
		//Get base url
		String baseUrl = TupleViewTest.getJettyURLBase(testableGraph, webContextOp);
		testContextURL(baseUrl, "cwc", cf1, cf2);
		testContextURL(baseUrl, "ctv", cf1, cf2);
		testContextURL(baseUrl, "cxv", cf1, cf2);
		testContextURL(baseUrl, "cpt", cf1, cf2);
		testContextURL(baseUrl, "cpx", cf1, cf2);
		testContextURL(baseUrl, "cpj", cf1, cf2);
		testContextURL(baseUrl, "cpb", cf1, cf2);
		testContextURL(baseUrl, "cwi", cf1, cf2);
		testContextURL(baseUrl, "cws", cf1, cf2);
		testContextURL(baseUrl, "crp", cf1, cf2);

		testableGraph.shutdown().get();
		cf1.delete();
	}
	
	private void testContextURL(String baseUrl, String context, File cf1, File cf2) throws Exception {
		URL url1 = new URL(baseUrl + "/" + context + "/" + cf1.getName());
		URL url2 = new URL(baseUrl + "/" + context + "/" + cf2.getName());
		
		System.out.println(url1.toString());
		HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
		System.out.println(conn1.getResponseMessage());
		assertEquals(HttpURLConnection.HTTP_OK, conn1.getResponseCode());
		conn1.disconnect();

		System.out.println(url2.toString());
		HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
		System.out.println(conn2.getResponseMessage());
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, conn2.getResponseCode());
		conn2.disconnect();
	}
}
