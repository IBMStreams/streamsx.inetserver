package com.ibm.streamsx.inet.wsserver.servlet;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import com.ibm.streams.operator.OperatorContext;

public class SendEventSocketCreator implements WebSocketCreator {

	private final SendEventSocket sendEventSocket;

	public SendEventSocketCreator(OperatorContext operatorContext, EventSocketConduit webSocketConduit) {
		sendEventSocket = new SendEventSocket(operatorContext, webSocketConduit);
	}

	@Override
	public Object createWebSocket(ServletUpgradeRequest arg0, ServletUpgradeResponse arg1) {
		return sendEventSocket;
	}
}
