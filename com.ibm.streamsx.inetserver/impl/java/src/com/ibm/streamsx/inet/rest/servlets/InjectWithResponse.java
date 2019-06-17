/**
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
*/

package com.ibm.streamsx.inet.rest.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.metrics.Metric;
import com.ibm.streamsx.inet.rest.ops.RequestProcess.RequestProcessConduit;
/**
 * <p>
 * Processes the message from the web and builds the response, timeout (Streams taking too long) are handled here as well. 
 * </p>
 */
public class InjectWithResponse extends SubmitterServlet {
	
	private static final long serialVersionUID = 1L;

	static final Logger trace = Logger.getLogger(InjectWithResponse.class.getName());
	
	private long webTimeout;
	private Map<Long, ReqWebMessage> activeRequests;
	private Metric nRequestTimeouts;
	private Metric nActiveRequests;
	private Function<ReqWebMessage,OutputTuple> tupleCreator;

	public InjectWithResponse(OperatorContext operatorContext, StreamingOutput<OutputTuple> port) {
		super(operatorContext, port);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		RequestProcessConduit requestProcessConduit = (RequestProcessConduit) config.getServletContext().getAttribute("operator.conduit");
		webTimeout = (long) (requestProcessConduit.webTimeout * 1000.0);
		activeRequests = requestProcessConduit.activeRequests;
		nRequestTimeouts = requestProcessConduit.nRequestTimeouts;
		nActiveRequests = requestProcessConduit.nActiveRequests;
		tupleCreator = requestProcessConduit.tupleCreator;
	}

	/**
	 * Build the web response and close the async context
	 * @throws IOException 
	 */
	@SuppressWarnings("deprecation")
	public static void buildWebResponse(ReqWebMessage exchangeWebMessage,
			String response, int statusCode, String statusMessage,
			Map<String, String> responseHeaders, String responseContentType) throws IOException {
		
		trace.debug("buildWebResponse - statusCode:" + statusCode + " contentType: " + responseContentType + " tracking key: " + exchangeWebMessage.getTrackingKey());
		AsyncContext asyncContext = exchangeWebMessage.getAsyncContext();
		HttpServletResponse webResponse = (HttpServletResponse)asyncContext.getResponse();
		
		if ((statusMessage == null) || statusMessage.isEmpty()) {
			webResponse.setStatus(statusCode);
		} else {
			webResponse.setStatus(statusCode, statusMessage);
		}
		
		// The jetty server seems to add more onto the contentType than I provided. 
		webResponse.setContentType(responseContentType);

		for (Iterator<String> iterator = responseHeaders.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			webResponse.setHeader(key, responseHeaders.get(key));
		}
		PrintWriter out = webResponse.getWriter();
		out.print(response);

		asyncContext.complete();
		return;
	}

	/**
	 * Build the web error response an close the async context
	 * @param exchangeWebMessage
	 * @param statusCode
	 * @param statusMessage
	 */
	public static void buildWebErrResponse(ReqWebMessage exchangeWebMessage, int statusCode, String statusMessage) {
		trace.debug("buildWebErrResponse - statusCode:" + statusCode + " tracking key: " + exchangeWebMessage.getTrackingKey());
		AsyncContext asyncContext = exchangeWebMessage.getAsyncContext();
		HttpServletResponse response = (HttpServletResponse)asyncContext.getResponse();

		try {
			if ((statusMessage == null) || statusMessage.isEmpty()) {
				response.sendError(statusCode);
			} else {
			response.sendError(statusCode, statusMessage);
			}
		} catch (IOException e) {
			trace.error(e.getMessage(), e);
		}

		asyncContext.complete();
	}

	/**
	 * Service an incoming http request.
	 * Create the ReqWebMessage and start the timeout.
	 * If this method is called from async context disable timeout and send no response
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final DispatcherType dispatcherType = request.getDispatcherType();
		final AsyncContext asyncContext = request.startAsync();
		if (dispatcherType.ordinal() == DispatcherType.ASYNC.ordinal()) {
			//the servlet was started from async timeout thread and must be closed without generating a response
			trace.debug("service dispatcherType == DispatcherType.ASYNC");
			asyncContext.setTimeout(0); //no new thread with this context will be started
		} else {
			//the servlet was started the first time set timeout supervision and event listener
			ReqWebMessage exchangeWebMessage = new ReqWebMessage(asyncContext);
			trace.info("service new trackingKey: " + exchangeWebMessage.getTrackingKey());
			asyncContext.setTimeout(webTimeout);
			InjectWithResponseListener myListener = new InjectWithResponseListener(exchangeWebMessage.getTrackingKey(), activeRequests, nRequestTimeouts, nActiveRequests);
			asyncContext.addListener(myListener);
			submit(tupleCreator.apply(exchangeWebMessage));
		}
	}

}