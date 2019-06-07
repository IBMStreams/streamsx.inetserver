/**
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019 
*/
package com.ibm.streamsx.inet.rest.servlets;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**.
* Bridge between the WWW request, Streams processing and corresponding WWW response..
* 
*/ 
public class ReqWebMessage {
	
	static final Logger trace = Logger.getLogger(InjectWithResponse.class.getName());
	private static AtomicLong trackingKeyGenerator = new AtomicLong();

	private final long trackingKey;
	private final HttpServletRequest request;
	private final AsyncContext asyncContext;

	/*
	 * tracking key, tracking each request, becomes the key on Streams side
	 * where it's used to correlate the request and response.
	 */

	public ReqWebMessage(HttpServletRequest request, AsyncContext asyncContext) {
		this.trackingKey = trackingKeyGenerator.incrementAndGet();
		this.request = request;
		this.asyncContext = asyncContext;
	}
	
	public long getTrackingKey() {
		return trackingKey;
	}
	// Servlet concept 
	public AsyncContext getAsyncContext() {
		return this.asyncContext;
	}
	public HttpServletRequest getRequest() {
		return request;
	}

}
