/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019 
 */
package com.ibm.streamsx.inet.rest.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.junit.Test;

import com.ibm.streams.flow.declare.InputPortDeclaration;
import com.ibm.streams.flow.declare.OperatorGraph;
import com.ibm.streams.flow.declare.OperatorGraphFactory;
import com.ibm.streams.flow.declare.OperatorInvocation;
import com.ibm.streams.flow.javaprimitives.JavaOperatorTester;
import com.ibm.streams.flow.javaprimitives.JavaTestableGraph;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.types.ValueFactory;
import com.ibm.streams.operator.types.XML;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.rest.ops.XMLView;

public class XMLViewTest {

    /**
     * Test an embedded jsonString attribute is converted
     * to JSON rather than a string that is serialized JSON.
     */
    @Test
    public void testXMLAttribute() throws Exception {
        System.out.println("testXMLAttribute()");
        OperatorGraph graph = OperatorGraphFactory.newGraph();

        OperatorInvocation<XMLView> op = graph.addOperator("TestXMLAttribute", XMLView.class);
        op.setIntParameter("port", 0);
        // Set the content to have a fixed URL
        op.setStringParameter("context", "XMLViewTest");
        op.setStringParameter("contextResourceBase", "/tmp"); // not actually serving any static content

        InputPortDeclaration tuplesToView = op.addInput("tuple<xml value>");
        
        // Create the testable version of the graph
        JavaTestableGraph testableGraph = new JavaOperatorTester().executable(graph);

        // Create the injector to inject test tuples.
        StreamingOutput<OutputTuple> injector = testableGraph.getInputTester(tuplesToView);
        
        // Execute the initialization of operators within graph.
        testableGraph.initialize().get().allPortsReady().get();
        
        //StringBuilder xstr = new StringBuilder("<x a=\"b\">55</x>");
        //XML xml = ValueFactory.newXML(xstr.)
        String xstring = new String("<x a=\"b\">55</x>");
        InputStream inputStream = new ByteArrayInputStream(xstring.getBytes(Charset.forName("UTF-8")));
        
        XML xdata = ValueFactory.newXML(inputStream);
        
        // and submit to the operator
        injector.submitAsTuple(xdata);
        
        URL url = new URL(getJettyURLBase(testableGraph, op) + "/XMLViewTest/TestXMLAttribute/ports/input/0/attribute");
        
        XML xresult = getXMLTuples(url);
        
        System.out.println("Result tuple: " + xresult.toString());
        assertEquals(xdata, xresult);

        testableGraph.shutdown().get();
    }
    
    /**
     * Get the server port from the operator's metric.
     */
    public static int getJettyPort(JavaTestableGraph tg,  OperatorInvocation<? extends ServletOperator> op) {
        return (int) tg.getOperatorInstance(op).getServerPort().getValue();
    }
    
    /**
     * Get the base part of the URL for a ServletOperator instance.
     */
    public static String getJettyURLBase(JavaTestableGraph tg,  OperatorInvocation<? extends ServletOperator> op) throws UnknownHostException {
        int port = getJettyPort(tg, op);
        String res = "http://" + InetAddress.getLocalHost().getHostName() + ":" + port;
        System.out.println("Using base url: " + res);
        return res;
    }
    
    /**
     * Get the tuples tuples from a URL assumed to be HTTPTupleView (TupleView.class).
     * @throws IOException 
     */
    public static XML getXMLTuples(URL url) throws IOException {
        System.out.println(url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        System.out.println("Response: " + conn.getResponseMessage());
        assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        assertTrue(conn.getContentType().startsWith("application/xml"));
        XML xml = ValueFactory.newXML(conn.getInputStream());
        conn.disconnect();
        return xml;
    }
}
