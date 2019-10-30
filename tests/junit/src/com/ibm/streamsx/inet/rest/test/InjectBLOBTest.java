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
import java.util.Arrays;
import java.util.Map;
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
import com.ibm.streams.operator.types.Blob;
import com.ibm.streamsx.inet.rest.ops.PostBLOB;

public class InjectBLOBTest {
	
	@Test
	public void testGoodOnlyXMLSchemaFirstPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.addOutput("tuple<blob data>");
		
		assertTrue(graph.compileChecks());
	}
	
	@Test
	public void testGoodXMLSchemaFirstPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.addOutput("tuple<blob data, rstring jsonString>");
		
		assertTrue(graph.compileChecks());
	}
	
	@Test
	public void testGoodXMLSchemaTwoPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.addOutput("tuple<blob data, rstring jsonString>");
		op.addOutput("tuple<blob data>");
		
		assertTrue(graph.compileChecks());
	}
	
	@Test
	public void testBadSchemaFirstPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.addOutput("tuple<int32 a>");
		
		assertFalse(graph.compileChecks());
	}
	
	@Test
	public void testBadSchemaSecondPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.addOutput("tuple<blob data, rstring jsonString>");
		op.addOutput("tuple<int32 a>");
		
		assertFalse(graph.compileChecks());
	}

	@Test
	public void testInjectSinglePort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare a HTTPJSONInjection operator
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples = op.addOutput("tuple<blob data>");
		
		// Create the testable version of the graph
		JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);
		
		MostRecent<Tuple> mrt = new MostRecent<Tuple>();
		testableGraph.registerStreamHandler(injectedTuples, mrt);

		// Execute the initialization of operators within graph.
		testableGraph.initialize().get().allPortsReady().get();
		
		assertNull(mrt.getMostRecentTuple());
		
		URL postTuple = new URL(TupleViewTest.getJettyURLBase(testableGraph, op) + "/" + op.getName() + "/ports/output/0/inject");
		
		// Make an XML POST request with an empty and an non empty XML string
		postBLOBAndTest(postTuple, new byte[] {}, mrt, false);

		postBLOBAndTest(postTuple, "<x a=\"b\">55</x>".getBytes(), mrt, false);

		testableGraph.shutdown().get();
	}

	@Test
	public void testInjectSingleNullPort() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare a HTTPJSONInjection operator
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples = op.addOutput("tuple<blob data, map<rstring,rstring> headers>");
		
		// Create the testable version of the graph
		JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);
		
		MostRecent<Tuple> mrt = new MostRecent<Tuple>();
		testableGraph.registerStreamHandler(injectedTuples, mrt);

		// Execute the initialization of operators within graph.
		testableGraph.initialize().get().allPortsReady().get();
		
		assertNull(mrt.getMostRecentTuple());
		
		URL postTuple = new URL(TupleViewTest.getJettyURLBase(testableGraph, op) + "/" + op.getName() + "/ports/output/0/inject");
		
		// Make an XML POST request with an empty and an non empty XML string
		postBLOBAndTest(postTuple, new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, mrt, true);

		testableGraph.shutdown().get();
	}

	@Test
	public void testInjectTwoPorts() throws Exception {
		OperatorGraph graph = OperatorGraphFactory.newGraph();

		// Declare a HTTPJSONInjection operator
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples0 = op.addOutput("tuple<blob data, rstring jsonString>");
		OutputPortDeclaration injectedTuples1 = op.addOutput("tuple<blob data>");

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
		postBLOBAndTest(postTuple0, "<x a=\"Hello\">32</x>".getBytes(), mrt0, false);
		assertNull(mrt1.getMostRecentTuple());
		
		URL postTuple1 = new URL(baseUrl + "/" + op.getName() + "/ports/output/1/inject");
		postBLOBAndTest(postTuple1, "<x attrib=\"99\">Goodbye</x>".getBytes(), mrt1, false);
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
		OperatorInvocation<PostBLOB> op = graph.addOperator(PostBLOB.class);
		op.setIntParameter("port", 0);
		
		OutputPortDeclaration injectedTuples = op.addOutput("tuple<blob data>");
		
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
			postBLOBAndTest(postTuple, s.getBytes(), mrt, false);
		} finally {

			testableGraph.shutdown().get();
		}
	}

	private static void postBLOBAndTest(URL postTuple, byte[] data, MostRecent<Tuple> mrt, boolean checkHeaderAttrib) throws IOException, InterruptedException {
		System.out.println(postTuple.toString());
		HttpURLConnection conn = (HttpURLConnection) postTuple.openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/octet-stream");
		conn.setRequestProperty("Content-Length", String.valueOf(data.length));
		OutputStream out = conn.getOutputStream();
		out.write(data);
		out.flush();
		out.close();
		System.out.println(conn.getResponseMessage());
		assertEquals(HttpURLConnection.HTTP_NO_CONTENT, conn.getResponseCode());
		conn.disconnect();
		Thread.sleep(1000);
		Blob tuple = mrt.getMostRecentTuple().getBlob(0);
		System.out.println(tuple.toString());
		assertTrue(Arrays.equals(data, tuple.getData()));
		
		if (checkHeaderAttrib) {
			@SuppressWarnings("unchecked")
			Map<String, String> headers = (Map<String, String>)mrt.getMostRecentTuple().getMap(1);
			System.out.println(headers.toString());
			assertTrue(headers.size() > 0);
		}
		mrt.clear();
	}
}
