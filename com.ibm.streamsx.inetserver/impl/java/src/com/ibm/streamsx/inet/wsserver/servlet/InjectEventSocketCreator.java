package com.ibm.streamsx.inet.wsserver.servlet;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import com.ibm.streams.operator.OperatorContext;

public class InjectEventSocketCreator implements WebSocketCreator {

	private final InjectEventSocket injectEventSocket;
	
	public InjectEventSocketCreator(OperatorContext operatorContext, EventSocketConduit webSocketConduit) {
		injectEventSocket = new InjectEventSocket(operatorContext, webSocketConduit);
	}

	@Override
	public Object createWebSocket(ServletUpgradeRequest arg0, ServletUpgradeResponse arg1) {
		return injectEventSocket;
	}

}
