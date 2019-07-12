package com.ibm.streamsx.inet.wsserver.servlet;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;

import com.ibm.streams.operator.metrics.Metric;
import com.ibm.streamsx.inet.wsserver.ops.WebSocketInject.WebMessageInfo;

/**
 * Conduit object between websocket operators and servlet.
 * [0] - function to create output tuple.
 * [1] - the synchronized list with connected sessions
 * [2] - the clients connected metric
 * [3] - the messages received metric
 * [4] - the acknowledgement count (0 means no ack)
 * [5] - enableConnectionControlMessages;
 */
public class EventSocketConduit {

	final public Consumer<WebMessageInfo> tupleCreator;
	final public Map<String, Session> sessionsConnected;
	final public Metric nClientsConnected;
	final public Metric nMessagesReceived;
	final public int ackCount;
	final public boolean enableConnectionControlMessages;

	public EventSocketConduit(
			Consumer<WebMessageInfo> tupleCreator, Map<String, Session> sessionsConnected, 
			Metric nClientsConnected, Metric nMessagesReceived, int ackCount, boolean enableConnectionControlMessages) {
		this.tupleCreator = tupleCreator;
		this.sessionsConnected = sessionsConnected;
		this.nClientsConnected = nClientsConnected;
		this.nMessagesReceived = nMessagesReceived;
		this.ackCount = ackCount;
		this.enableConnectionControlMessages = enableConnectionControlMessages;
	}

}
