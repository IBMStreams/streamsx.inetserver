/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2014 
*/
package com.ibm.streamsx.inet.rest.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.junit.Test;

import com.ibm.streams.flow.declare.OperatorGraph;
import com.ibm.streams.flow.declare.OperatorGraphFactory;
import com.ibm.streams.flow.declare.OperatorInvocation;
import com.ibm.streams.flow.declare.OutputPortDeclaration;
import com.ibm.streams.flow.handlers.MostRecent;
import com.ibm.streams.flow.javaprimitives.JavaOperatorTester;
import com.ibm.streams.flow.javaprimitives.JavaTestableGraph;
import com.ibm.streams.operator.Tuple;
import com.ibm.streamsx.inet.rest.ops.PostXML;

public class InjectXMLTest {
	
	@Test
	public void testGoodOnlyXMLSchemaFirstPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.addOutput("tuple<xml data>");
		
		assertTrue(graph.compileChecks());
	}
	
	@Test
	public void testGoodXMLSchemaFirstPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.addOutput("tuple<xml data, rstring jsonString>");
		
		assertTrue(graph.compileChecks());
	}
	
	@Test
	public void testGoodXMLSchemaTwoPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.addOutput("tuple<xml data, rstring jsonString>");
		op.addOutput("tuple<xml data>");
		
		assertTrue(graph.compileChecks());
	}
	
	@Test
	public void testBadSchemaFirstPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.addOutput("tuple<int32 a>");
		
		assertFalse(graph.compileChecks());
	}
	
	@Test
	public void testBadSchemaSecondPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.addOutput("tuple<xml data, rstring jsonString>");
		op.addOutput("tuple<int32 a>");
		
		assertFalse(graph.compileChecks());
	}

	@Test
	public void testInjectSinglePort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare a HTTPJSONInjection operator
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples = op.addOutput("tuple<xml data>");
		
		// Create the testable version of the graph
		JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);
		
		MostRecent<Tuple> mrt = new MostRecent<Tuple>();
		testableGraph.registerStreamHandler(injectedTuples, mrt);

		// Execute the initialization of operators within graph.
		testableGraph.initialize().get().allPortsReady().get();
		
		assertNull(mrt.getMostRecentTuple());
		
		URL postTuple = new URL(TupleViewTest.getJettyURLBase(testableGraph, op) + "/" + op.getName() + "/ports/output/0/inject");
		
		// Make an XML POST request with an empty and an non empty XML string
		postXMLAndTest(postTuple, "", mrt);

		postXMLAndTest(postTuple, "<x a=\"b\">55</x>", mrt);

		testableGraph.shutdown().get();
	}

	@Test
	public void testInjectTwoPorts() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare a HTTPJSONInjection operator
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples0 = op.addOutput("tuple<xml data, rstring jsonString>");
		OutputPortDeclaration injectedTuples1 = op.addOutput("tuple<xml data>");

		// Create the testable version of the graph
		JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);
		
		MostRecent<Tuple> mrt0 = new MostRecent<Tuple>();
		testableGraph.registerStreamHandler(injectedTuples0, mrt0);
		
		MostRecent<Tuple> mrt1 = new MostRecent<Tuple>();
		testableGraph.registerStreamHandler(injectedTuples1, mrt1);

		// Execute the initialization of operators within graph.
		testableGraph.initialize().get().allPortsReady().get();
		
		//Get bas url
		String baseUrl = TupleViewTest.getJettyURLBase(testableGraph, op);
		URL postTuple0 = new URL(baseUrl + "/" + op.getName() + "/ports/output/0/inject");
		postXMLAndTest(postTuple0, "<x a=\"Hello\">32</x>", mrt0);
		assertNull(mrt1.getMostRecentTuple());
		
		URL postTuple1 = new URL(baseUrl + "/" + op.getName() + "/ports/output/1/inject");
		postXMLAndTest(postTuple1, "<x attrib=\"99\">Goodbye</x>", mrt1);
		assertNull(mrt0.getMostRecentTuple());

		testableGraph.shutdown().get();
	}

	@Test
	public void testBigInjectFails() throws Exception {	
		// Make an XML POST request with an 800KB+ JSON object
		_testBigInject(800);
	}
	
	public void _testBigInject(int nk) throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare a HTTPJSONInjection operator
		OperatorInvocation<PostXML> op = graph.addOperator(PostXML.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples = op.addOutput("tuple<xml data>");
		
		// Create the testable version of the graph
		JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);
		
		MostRecent<Tuple> mrt = new MostRecent<Tuple>();
		testableGraph.registerStreamHandler(injectedTuples, mrt);

		// Execute the initialization of operators within graph.
		testableGraph.initialize().get().allPortsReady().get();
		
		assertNull(mrt.getMostRecentTuple());
		
		URL postTuple = new URL(TupleViewTest.getJettyURLBase(testableGraph, op) + "/" + op.getName() + "/ports/output/0/inject");
		
		try {

			Random r = new Random();
			char[] chars = new char[nk * 1000];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = (char) ('a' + (char) r.nextInt(26));
			}
			String s = "<x>" + new String(chars) + "</x>";
			postXMLAndTest(postTuple, s, mrt);
		} finally {

			testableGraph.shutdown().get();
		}
	}

	private static void postXMLAndTest(URL postTuple, String xml, MostRecent<Tuple> mrt) throws IOException {
		System.out.println(postTuple.toString());
		byte[] dataBytes = xml.getBytes("UTF-8");
		HttpURLConnection conn = (HttpURLConnection) postTuple.openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/xml");
		conn.setRequestProperty("Content-Length", String.valueOf(dataBytes.length));
		OutputStream out = conn.getOutputStream();
		out.write(dataBytes);
		out.flush();
		out.close();
		System.out.println(conn.getResponseMessage());
		assertEquals(HttpURLConnection.HTTP_NO_CONTENT, conn.getResponseCode());
		conn.disconnect();
		
		String tuple = mrt.getMostRecentTuple().getString(0);
		System.out.println(tuple.toString());
		assertEquals(xml, tuple);
		
		mrt.clear();
	}

}
