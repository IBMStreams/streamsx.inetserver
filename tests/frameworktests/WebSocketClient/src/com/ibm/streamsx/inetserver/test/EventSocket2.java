package com.ibm.streamsx.inetserver.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class EventSocket2 extends WebSocketAdapter {
	
	private static AtomicLong sessioncounter = new AtomicLong();
	private String filename = null;
	private Writer fw = null;
	private int messagesReceived = 0;
	
	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);
		System.out.println("Socket Connected: " + connId());
		filename = WsClient2.directory + "/WsData_" + sessioncounter.incrementAndGet() + ".txt";
		try {
			fw = new FileWriter(filename);
			fw.write(connId() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onWebSocketText(String message) {
		super.onWebSocketText(message);
		System.out.println("Received TEXT " + connId() + " message: " + message);
		messagesReceived++;
		if (fw != null)
			try {
				fw.write(message + "\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		if (messagesReceived > 100) {
			if (fw != null)
				try {
					fw.close();
					fw = null;
					Writer fw2 = new FileWriter(filename + ".end");
					fw2.write("END");
					fw2.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		super.onWebSocketBinary(payload, offset, len);
		System.out.println("Received BINARY " + connId() + " len: " + (len - offset));
		messagesReceived++;
		if (fw != null)
			try {
				String mess = new String(payload, offset, len);
				fw.write("offset: " + offset + " len: " + len + " " + mess + "\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		if (messagesReceived > 100) {
			if (fw != null)
				try {
					fw.close();
					fw = null;
					Writer fw2 = new FileWriter(filename + ".end");
					fw2.write("END");
					fw2.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
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
