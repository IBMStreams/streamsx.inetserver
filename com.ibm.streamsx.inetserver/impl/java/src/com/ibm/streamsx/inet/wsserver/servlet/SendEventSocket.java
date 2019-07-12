package com.ibm.streamsx.inet.wsserver.servlet;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.ibm.streams.operator.OperatorContext;

@WebSocket
public class SendEventSocket extends EventSocket {

	static final Logger trace = Logger.getLogger(SendEventSocket.class.getName());
	
	public SendEventSocket(OperatorContext operatorContext, EventSocketConduit webSocketConduit) {
		super(operatorContext, webSocketConduit);
	}

	@OnWebSocketMessage
	public void onWebSocketText(Session session, String message) {
		super.onWebSocketText(session, message); //trace
	}
	
	@OnWebSocketMessage
	public void onWebSocketBinary(Session session, byte[] payload, int offset, int len) {
		super.onWebSocketBinary(session, payload, offset, len); //trace
	}

}
