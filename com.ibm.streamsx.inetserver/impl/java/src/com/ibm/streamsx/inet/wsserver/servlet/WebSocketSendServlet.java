package com.ibm.streamsx.inet.wsserver.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.ibm.streams.operator.OperatorContext;

@SuppressWarnings("serial")
public class WebSocketSendServlet extends WebSocketServlet {

	private final OperatorContext operatorContext;
	private EventSocketConduit webSocketConduit;

	public WebSocketSendServlet(OperatorContext operatorContext) {
		this.operatorContext = operatorContext;
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		webSocketConduit = (EventSocketConduit) config.getServletContext().getAttribute("operator.conduit");
		//this seems to call configure
		super.init(config);
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.setCreator(new SendEventSocketCreator(operatorContext, webSocketConduit));
	}

	
}
