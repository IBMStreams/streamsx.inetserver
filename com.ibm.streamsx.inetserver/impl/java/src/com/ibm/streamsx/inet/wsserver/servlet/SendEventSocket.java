package com.ibm.streamsx.inet.wsserver.servlet;

import org.apache.log4j.Logger;

import com.ibm.streams.operator.OperatorContext;

public class SendEventSocket extends EventSocket {

	static final Logger trace = Logger.getLogger(SendEventSocket.class.getName());
	
	public SendEventSocket(OperatorContext operatorContext, EventSocketConduit webSocketConduit) {
		super(operatorContext, webSocketConduit);
	}

}
