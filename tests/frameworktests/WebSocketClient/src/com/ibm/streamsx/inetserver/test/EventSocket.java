package com.ibm.streamsx.inetserver.test;

import java.io.IOException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class EventSocket extends WebSocketAdapter {
	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);
		System.out.println("Socket Connected: " + sess);
	}
	
	@Override
	public void onWebSocketText(String message) {
		super.onWebSocketText(message);
		System.out.println("Received TEXT message: " + message);
		if (isConnected()) {
			if (message.startsWith("Hello")) {
				try {
					getRemote().sendString("Answer");
				} catch (IOException e) {
					System.out.println("IOException");
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		super.onWebSocketBinary(payload, offset, len);
		System.out.println("onWebSocketBinary");
	}
	
	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode,reason);
		System.out.println("Socket Closed: [" + statusCode + "] " + reason);
	}
	
	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace(System.err);
	}
}
