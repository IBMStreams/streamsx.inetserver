/*
 * Copyright 2019 IBM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.streamsx.inet.wsserver.servlet;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;

import com.ibm.json.java.JSONObject;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.log4j.TraceLevel;
import com.ibm.streams.operator.metrics.Metric;

/**
 * The common Socket class for websocket operators
 * This class handles the common websocket events and maintains the
 * logical connection the operators with the internal attributes:
 * operatorContext, sessionsConnected, nClientsConnected
 * The sessionsConnected set lists all connected client sessions and maps 
 * the immutable session key hostIp:port to the mutable Session object.
 */
public class EventSocket {

	public static final String N_CLIENTS_CONNRCTED_DESCR = 
			"Number of clients currently connected to the operators WebSocket context.";

	private static final Logger trace = Logger.getLogger(EventSocket.class.getName());
	
	protected final OperatorContext operatorContext;
	private final Map<String, Session> sessionsConnected;
	private final Metric nClientsConnected;
	private final boolean enableConnectionControlMessages;

	public EventSocket(OperatorContext operatorContext, EventSocketConduit webSocketConduit) {
		super();
		this.operatorContext = operatorContext;
		this.sessionsConnected = webSocketConduit.sessionsConnected;
		this.nClientsConnected = webSocketConduit.nClientsConnected;
		this.enableConnectionControlMessages = webSocketConduit.enableConnectionControlMessages;
		if (trace.isEnabledFor(TraceLevel.INFO)) trace.info(operatorContext.getLogicalName() + ": EventSocket(...)");
	}

	@OnWebSocketConnect
	public synchronized void onWebSocketConnect(Session session) {
		if (trace.isEnabledFor(TraceLevel.INFO))
			trace.info(operatorContext.getLogicalName() + ": onWebSocketConnect - remote: " + getRemoteId(session));
	
		String sessionKey = getSessionKey(session);
		Session previousSession = sessionsConnected.put(sessionKey, session);
		if (previousSession != null) {
			trace.error(operatorContext.getLogicalName() + ": onWebSocketConnect - duplicate session (connect) key: " 
					+ sessionKey + " remote: " + getRemoteId(session));
		}

		nClientsConnected.setValue(sessionsConnected.size());
		if (enableConnectionControlMessages) statusToAll( "OPEN",  "R:" + getRemoteId(session) + " L:" + session.getLocalAddress());
	}

	@OnWebSocketClose
	public synchronized void onWebSocketClose(Session session, int statusCode, String reason) {
		if (trace.isEnabledFor(TraceLevel.INFO))
			trace.info(operatorContext.getLogicalName() + ": onWebSocketClose - remote: " + getRemoteId(session) +
					" statusCode: " + statusCode + " reason: " + reason);
		
		String sessionKey = getSessionKey(session);
		if ( sessionsConnected.remove(sessionKey) == null) {
			trace.error(operatorContext.getLogicalName() + ": onWebSocketClose - can not remove session key - : " + sessionKey +
					" statusCode: " + statusCode + " reason: " + reason);
		}
		
		nClientsConnected.setValue(sessionsConnected.size());
		if (enableConnectionControlMessages) statusToAll( "CLOSE",  "R:" + getRemoteId(session) + " L:" + session.getLocalAddress());
	}

	@OnWebSocketError
	public void onWebSocketError(Session session, Throwable cause) {
		trace.error(operatorContext.getLogicalName() + ": onWebSocketError - remote: " + getRemoteId(session) + " mause: " + cause.getMessage(), cause);
	}

	protected void onWebSocketText(Session session, String message) {
		if (trace.isEnabledFor(TraceLevel.INFO)) trace.info(operatorContext.getLogicalName() + ": onWebSocketText - remote: " + getRemoteId(session) + " length: " + message.length());
	}
	
	protected void onWebSocketBinary(Session session, byte[] payload, int offset, int len) {
		if (trace.isEnabledFor(TraceLevel.INFO)) trace.info(operatorContext.getLogicalName() + ": onWebSocketBinary - remote: " + getRemoteId(session) + " length: " + (len - offset));
	}

	static public String getRemoteId(Session sess) {
		String res = "null";
		if (sess != null) {
			res = sess.getRemote().getInetSocketAddress().getAddress().getHostAddress() + ":" + sess.getRemote().getInetSocketAddress().getPort() + " " + sess.getRemote().getBatchMode().name();
		}
		return res;
	}

	static public String getSessionKey(Session sess) {
		return sess.getRemote().getInetSocketAddress().getAddress().getHostAddress() + ":" + sess.getRemote().getInetSocketAddress().getPort();
	}

	/**
	 * Send a status/control message to all, since we have control and data on the same 
	 * interface need a consistent way to send such a message. 
	 * @param status
	 * @param text
	 */
	protected void statusToAll(String status, String text) {
		JSONObject controlMessage = new JSONObject();
		JSONObject controlBody = new JSONObject();
		controlBody.put("status", status);
		controlBody.put("value", text);
		controlMessage.put("control", controlBody);
		sendToAll(controlMessage);
	}

	/**
	 * Sends <var>jsonMessage</var> to all currently connected WebSocket clients.
	 * 
	 * @param jsonMessage to transmit 
	 * 
	 * @return number of messages sent, thus the number of active connections.
	 */
	private int sendToAll( JSONObject jsonMessage ) {
		String message = null;
		int cnt = 0;
		try {
			message = jsonMessage.serialize();
			if (trace.isEnabledFor(TraceLevel.TRACE)) trace.log(TraceLevel.TRACE, operatorContext.getLogicalName() +  ": sendToAll() : " + message);
			if (message != null) {
				synchronized(sessionsConnected) { //this method is used aslo from the non synced incrementAndAck (InjectEventSocket)
					Collection<Session> sessions = sessionsConnected.values();
					for( Session session : sessions ) {
						if (session.isOpen()) {
							if (trace.isEnabledFor(TraceLevel.TRACE)) trace.log(TraceLevel.TRACE, operatorContext.getLogicalName() +  ": sendToAll(): " + getRemoteId(session));
							session.getRemote().sendString(message);
						} else {
							trace.error(operatorContext.getLogicalName() +  ": sendToAll(): " + getRemoteId(session) + " session aready closed");
						}
					}
				}
			}
			return cnt;
		} catch (Exception e) { //catch all exceptions to avoid errors with concurrently closed sessions
			trace.error(operatorContext.getLogicalName() + ": sendToAll(): " + jsonMessage.toString()
			+ " err: " + e.getClass().getName() + " " + e.getMessage(), e);
		}
		return cnt;
	}

}
