package com.ibm.streamsx.inetserver.test;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class EventSocket1 extends WebSocketAdapter {
	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);
		System.out.println("Socket Connected: " + connId());
	}
	
	@Override
	public void onWebSocketText(String message) {
		super.onWebSocketText(message);
		System.out.println("Received TEXT " + connId() + " message: " + message);
	}
	
	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		super.onWebSocketBinary(payload, offset, len);
		System.out.println("Received BINARY " + connId() + " len: " + (len - offset));
	}
	
	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode,reason);
		System.out.println("Socket Closed: " + connId() + " [" + statusCode + "] " + reason);
	}
	
	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		System.out.println("Socket ERROR: " + connId());
		cause.printStackTrace(System.err);
	}
	
	private String connId() {
		Session session = getSession();
		String result = "null";
		if (session != null) {
			result = session.getLocalAddress().getAddress().getHostAddress() + ":" + session.getLocalAddress().getPort() + " " + session.getRemote().getBatchMode();
		}
		return result;
	}
}
