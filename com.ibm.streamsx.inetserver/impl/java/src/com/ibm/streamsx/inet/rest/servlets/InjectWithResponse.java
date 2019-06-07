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
/**
 * <p>
 * Processes the message from the web and builds the response, timeout (Streams taking too long) are handled here as well. 
 * </p>
 * <p>
 * Dependent on how Jetty drives the interaction.
 * Web request arrives via Jetty, the actions that ensue are dependent on the state of the request's state: 
 *</p>
 * <ul>
 *  <li>Initial : A new request has arrived, move the request into a Streams tuple, the web request is suspended. 
 *     The suspended request pushed back to Jetty with a timeout. </li>
 * 	<li>Resumed : Streams has finished processing the request and the answer is ready to be returned, the suspended web request
 * has been continued by the arrival of the tuple on the operators Input port.  All the initial web request values are available. 
 * The response is built from the arriving tuple and sent out the web.   </li>   
 * 	<li>Expired : Streams has taken too long and the request has expired, generate a timeout response.</li> 
 * 	<li>Suspend : Streams is still working on the request, this should not happen.</li>
 * </ul>
 *
 */
public class InjectWithResponse extends SubmitterServlet {
	
	private static final long serialVersionUID = 1L;

	public static final String METRIC_NAME_REQUEST_TIMEOUT = "nRequestTimeouts";
	public static final String METRIC_NAME_ACTIVE_REQUESTS = "nActiveRequests";
	
	static final Logger trace = Logger.getLogger(InjectWithResponse.class.getName());
	
	// Number of timeouts that have occurred. Used on timeout error message, attempting to 
	// hint where a possible problem is occurring. 
	public static int timeoutCount = 0;

	interface Constant {
		public static final String EXCHANGEWEBMESSAGE = "exchangeWebMessage";
	}

	private final long webTimeout;
	private final Map<Long, ReqWebMessage> activeRequests;
	private final Metric nRequestTimeouts;
	private final Metric nActiveRequests;
	
	private Function<ReqWebMessage,OutputTuple> tupleCreator;

	public InjectWithResponse(OperatorContext operatorContext, StreamingOutput<OutputTuple> port, double webTimeout, Map<Long, ReqWebMessage> activeRequests) {
		super(operatorContext, port);
		this.webTimeout = (long) (webTimeout * 1000.0);
		this.activeRequests = activeRequests;
		this.nRequestTimeouts = operatorContext.getMetrics().getCustomMetric(METRIC_NAME_REQUEST_TIMEOUT);
		this.nActiveRequests = operatorContext.getMetrics().getCustomMetric(METRIC_NAME_ACTIVE_REQUESTS);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		tupleCreator = (Function<ReqWebMessage, OutputTuple>) config.getServletContext().getAttribute("operator.conduit");
	}

	/**
	 * Build the web response and close the async context
	 * @throws IOException 
	 */
	public static void buildWebResponse(ReqWebMessage exchangeWebMessage,
			String response, int statusCode, String statusMessage,
			Map<String, String> responseHeaders, String responseContentType) throws IOException {
		
		trace.info("buildWebResponse - statusCode:" + statusCode );
		AsyncContext asyncContext = exchangeWebMessage.getAsyncContext();
		HttpServletResponse webResponse = (HttpServletResponse)asyncContext.getResponse();
		
		if (statusCode >= HttpServletResponse.SC_BAD_REQUEST) {
			trace.info("buildWebResponse error - statusCode:" + statusCode );
		} else {
			trace.info("buildWebResponse : statusCode: " + statusCode);
		}
		if (statusMessage != null) {
			webResponse.sendError(statusCode, statusMessage);
		} else {
			webResponse.sendError(statusCode);
		}
		
		trace.info("buildWebResponse : contentType: " + responseContentType);
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
	 * @param errCode
	 * @throws IOException
	 */
	public static void buildWebErrResponse(ReqWebMessage exchangeWebMessage, int errCode) throws IOException {
		trace.warn("buildWebErrResponse - errCode:" + errCode + " tracking key: " + exchangeWebMessage.getTrackingKey());
		AsyncContext asyncContext = exchangeWebMessage.getAsyncContext();
		HttpServletResponse response = (HttpServletResponse)asyncContext.getResponse();

		response.setContentType("text/html; charset=utf-8"); // this should be
		PrintWriter out = response.getWriter();
		response.setStatus(errCode);
		if (errCode == HttpServletResponse.SC_REQUEST_TIMEOUT) {
			out.print("<h1>Request timeout</h1>");
		} else {
			out.print("<h1>Unanticipated error : " + errCode +  "</h1>");
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
			trace.info("service dispatcherType == DispatcherType.ASYNC");
			asyncContext.setTimeout(0); //no new thread with this context will be started
		} else {
			//the servlet was started the first time set timeout supervision and event listener
			ReqWebMessage exchangeWebMessage = new ReqWebMessage(request, asyncContext);
			trace.info("service new trackingKey: " + exchangeWebMessage.getTrackingKey());
			asyncContext.setTimeout(webTimeout);
			InjectWithResponseListener myListener = new InjectWithResponseListener(exchangeWebMessage.getTrackingKey(), activeRequests, nRequestTimeouts, nActiveRequests);
			asyncContext.addListener(myListener);
			submit(tupleCreator.apply(exchangeWebMessage));
		}
	}

}