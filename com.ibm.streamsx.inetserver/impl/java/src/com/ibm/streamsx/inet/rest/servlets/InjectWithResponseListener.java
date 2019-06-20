/**
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019
*/

package com.ibm.streamsx.inet.rest.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.ibm.streams.operator.metrics.Metric;

public class InjectWithResponseListener implements AsyncListener {
	
	private static final Logger trace = Logger.getLogger(InjectWithResponseListener.class.getName());

	private final Long trackingKey;
	private final Map<Long, ReqWebMessage> activeRequests;
	private final Metric nRequestTimeouts;
	private final Metric nActiveRequests;
	
	public InjectWithResponseListener(long trackingKey, Map<Long, ReqWebMessage> activeRequests, Metric nRequestTimeouts, Metric nActiveRequests) {
		this.trackingKey = trackingKey;
		this.activeRequests = activeRequests;
		this.nRequestTimeouts = nRequestTimeouts;
		this.nActiveRequests = nActiveRequests;
	}
	
	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		trace.debug("onComplete: trackingKey: " + trackingKey);
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		trace.error("onError: trackingKey: " + trackingKey);
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
		trace.debug("onStartAsync: trackingKey: " + trackingKey);
	}

	/**
	 * checks whether the tracking key is still activeRequests map
	 * if so: generate the timeout response and remove the tracking key from activeRequests map
	 */
	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		trace.debug("onTimeout trackingKey: " + trackingKey);
		ReqWebMessage reqWebMessage = activeRequests.remove(trackingKey);
		if (reqWebMessage == null) {
			//the tracking key was already processed from operator process method -> do nothing
			trace.debug("onTimeout trackingKey: " + trackingKey + " not in activeRequests -> no action required");
			//dispatch (and close) this async context again and set the timeout to 0 -> done
			event.getAsyncContext().dispatch();
		} else {
			trace.error("onTimeout trackingKey: " + trackingKey + " timeout");
			nRequestTimeouts.increment();
			nActiveRequests.setValue(activeRequests.size());
			//send response and close the async context here
			InjectWithResponse.buildWebErrResponse(reqWebMessage, HttpServletResponse.SC_REQUEST_TIMEOUT, null);
		}
	}

}
