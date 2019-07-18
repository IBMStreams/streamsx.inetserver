package com.ibm.streamsx.inet.wsserver.servlet;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.metrics.Metric;
import com.ibm.streamsx.inet.wsserver.ops.WebSocketInject.WebMessageInfo;

@WebSocket
public class InjectEventSocket extends EventSocket {

	public static final String N_MESSAGES_RECEIVED_DESCR =
			"Number of messages received via WebSocket. (cumulative sum for all connected clients)";

	static final Logger trace = Logger.getLogger(InjectEventSocket.class.getName());

	private final Consumer<WebMessageInfo> tupleCreator;
	private final Metric nMessagesReceived;
	private final int ackCount;
	
	private AtomicLong messagesReceived;

	public InjectEventSocket(OperatorContext operatorContext, EventSocketConduit webSocketConduit) {
		super(operatorContext, webSocketConduit);
		this.tupleCreator = webSocketConduit.tupleCreator;
		this.nMessagesReceived = webSocketConduit.nMessagesReceived;
		this.ackCount = webSocketConduit.ackCount;
		messagesReceived = new AtomicLong();
	}

	@OnWebSocketMessage
	public void onWebSocketText(Session session, String message) {
		super.onWebSocketText(session, message); //trace

		WebMessageInfo webMessageInfo = new WebMessageInfo(message, null, 0, 0, getRemoteId(session));
		tupleCreator.accept(webMessageInfo);

		incrementAndAck();
	}
	
	@OnWebSocketMessage
	public void onWebSocketBinary(Session session, byte[] payload, int offset, int len) {
		super.onWebSocketBinary(session, payload, offset, len); //trace
		
		WebMessageInfo webMessageInfo = new WebMessageInfo(null, payload, offset, len, getRemoteId(session));
		tupleCreator.accept(webMessageInfo);
		
		incrementAndAck();
	}

	private void incrementAndAck() {
		long nrec = messagesReceived.incrementAndGet();
		nMessagesReceived.setValue(nrec);
		
		if ((ackCount != 0) && (nrec % this.ackCount) == 0) {
			statusToAll("COUNT", String.format("%d",nrec));
		}
	}

}
