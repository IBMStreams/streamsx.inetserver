/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019
*/
package com.ibm.streamsx.inet.rest.ops;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.ibm.json.java.JSONObject;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamSchema;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streams.operator.Type.MetaType;
import com.ibm.streams.operator.log4j.TraceLevel;
import com.ibm.streams.operator.metrics.Metric;
import com.ibm.streams.operator.metrics.Metric.Kind;
import com.ibm.streams.operator.model.CustomMetric;
import com.ibm.streams.operator.model.Icons;
import com.ibm.streams.operator.model.InputPortSet;
import com.ibm.streams.operator.model.InputPortSet.WindowMode;
import com.ibm.streams.operator.model.InputPortSet.WindowPunctuationInputMode;
import com.ibm.streams.operator.model.InputPorts;
import com.ibm.streams.operator.model.OutputPortSet;
import com.ibm.streams.operator.model.OutputPortSet.WindowPunctuationOutputMode;
import com.ibm.streams.operator.model.OutputPorts;
import com.ibm.streams.operator.model.Parameter;
import com.ibm.streams.operator.model.PrimitiveOperator;
import com.ibm.streams.operator.types.RString;
import com.ibm.streamsx.inet.rest.servlets.InjectWithResponse;
import com.ibm.streamsx.inet.rest.servlets.ReqWebMessage;

/**
 * <p>
 * HTTPRequestProcess - Enable Streams to process web requests . A request arrives via the web which injects a
 * tuple out the output port; processing happens; input port receives the processing results which are
 * communicated to the originating web requester. This is a gateway between web requests and Streams processing of the requests.
 * </p>
 * <p>
 * This relies on an embedded Jetty server. The Jetty portion: accepts a request from the web, starts the AsyncContext of the web request 
 * while Stream's processes the request, continues the web connection when Streams completes the processing and finally responds to 
 * the original request. 
 * </p>
 * <p>
 * The following is a brief description of the classes and their interaction. 
 * </p>
 * <dl>
 * <dt>{@link HTTPRequestProcess}</dt>
 * <dd>
 * Operator Entry.  Web requests for Streams are injected into the Streams via the operators Output port. Responses, 
 * completed requests enter via the Input port. The request and response are packaged into ReqWebMessage objects.
 * </dd>
 * </dl>
 * <p>
 * This operator generates a key attribute on the output port and expects the same key attribute 
 * value on the input port, this is correlation key. If keys is corrupted, no response will be generated, 
 * the request will time out. 
 * </p> 
 *
 */
@PrimitiveOperator(name = "HTTPRequestProcess", description = RequestProcess.DESC)
@InputPorts({
		@InputPortSet(description = "Response to be returned to the web requestor.", cardinality = 1, optional = false, controlPort=true, windowingMode = WindowMode.NonWindowed, windowPunctuationInputMode = WindowPunctuationInputMode.Oblivious)})
@OutputPorts({
		@OutputPortSet(description = "Request from web to process.", cardinality = 1, optional = false, windowPunctuationOutputMode = WindowPunctuationOutputMode.Free) })
@Icons(location32="icons/HTTPTupleRequest_32.jpeg", location16="icons/HTTPTupleRequest_16.jpeg")

public class RequestProcess extends ServletOperator {
	static Logger trace = Logger.getLogger(RequestProcess.class.getName());

	// communication
	public static final String defaultKeyAttributeName = "key";
	public static final String defaultJsonAttributeName = "jsonString";
	public static final String defaultContentTypeAttributeName = "contentType";
	public static final String defaultHeaderAttributeName = "header";

	public static final String defaultResponseAttributeName = "response";
	public static final String defaultResponseStatusAttributeName = "status";

	public static final String defaultRequestAttributeName = "request";
	public static final String defaultContextPathAttributeName = "contextPath";
	public static final String defaultUrlAttributeName = "url";
	public static final String defaultMethodAttributeName = "method";	
	public static final String defaultPathInfoAttributeName = "pathInfo";
	//public static final String defaultContext = "/streams";
	
	public static final String defaultResponseContentType = "text/html; charset=utf-8";
	public static final int defaultStatusCode = HttpServletResponse.SC_OK;

	static final String DESC = "Operator accepts a web request and generates corresponding response.  The request is injected into "
			+ "streams on the output port, the input port receives the response."
			+ "This enables a developer to process HTTP form's and REST calls. The request arrives on the output port, results are " 
			+ "presented on the input port."
			+ "The request is correlated to the response with an attribute 'key' that arrives with the request parameters' on the output port "
			+ "and must accompany the response on the input port."
			+ "\\n\\n" 
			+ "The URLs defined by this operator are:\\n"
			+ "* *prefix*`/ports/analyze/`*port index*`/` - Injects a tuple into the output and the response is taken from the matching tuple on the input port.\\n"
			+ "* *prefix*`/ports/input/`*port index*`/info` - Output port meta-data including the stream attribute names and types (content type `application/json`).\\n"
			+ "\\nThe *prefix* for the URLs is:\\n"
			+ "* *context path*`/`*base operator name* - When the `context` parameter is set.\\n"
			+ "* *full operator name* - When the `context` parameter is **not** set.\\n"
			+ "\\n"
			+ "For the `analyze` path any HTTP method can be used and any sub-path. For example with a context of "
			+ "`api` and operator name of `Bus` then `api/Bus/ports/analyze/0/get_location` is valid."
			+ "\\n\\n"
			+ "Input and output ports have two possible formats: tuple and json. The modes of input- and output port are independant."
			+ "A port works in json mode if it has one attribute with name jsonString. "
			+ "With tuple format, each web input fields is mapped to an attribute. "
			+ "Json format has one attribute ('jsonString'), each web field is mapped to a json object field. "
			+ "\\n\\n"
			+ "The jsonString object will be populated with all the fields. The default attribute names can be "
			+ "overridden for tuple. "
			+ "\\n\\n" 
			+ "The operator handles two flavors of http requests, forms and REST. In the case of forms, webpages can be served up from the contextResourceBase, "
			+ "this can be to static html or template. . Refer to the spl example for a form processed by the operator using a template to format the response."
			+ "\\n\\n "
			+ "For the input port (the web response), only the 'key' is mandatory for both json and tuple."
			+ "\\n\\n"
			+ "For the output port in tuple mode (the web request), only the 'key' attribute is mandatory. All other attributes "
			+ "'request', 'method', 'header', 'contentType', 'pathInfo', 'contextPath' and 'url' are optional. In json mode all "
			+ "attributes are propagated to the json attribute "
			+ "\\n\\n"
			+ "The following lists the default values if the field or attribute is not provided.\\n"
			+ "* rstring response : 0 length response.\\n"
			+ "* int32 statusCode : 200 (OK)\\n"
			+ "* rstring statusMessage :  not set\\n"
			+ "* rstring contentType : '" + defaultResponseContentType + "'.\\n"
			+ "* Map<rstring,rstring> headers : No headers provided\\n "
			+ "\\n\\n "
			+ "# Notes:\\n\\n "
			+ "* The 'key' attribute on the output and input port's are correlated. Losing the correlation loses the request.\\n "
			+ "* If the input port's response key cannot be located the web request will timeout, metrics will be incremented.\\n "
			+ "* If the input jsonString value cannot be converted to an jsonObject, no response will be generated and web request will timeout.\\n "
			+ "* Only the first input port's key will produce a web response.\\n "
			+ "* The 'jsonString' attribute json field names are the default attribute names.\\n "
			+ "* context/pathInfo relationship : A request's context path beyond the base is accepted, the 'pathInfo' attribute will have path beyond the base.  "
			+ "  If the context path is */work* requests to */work/translate* will have a 'pathInfo' of */translate* and requests "
			+ "  to */work/translate/speakeasy* will have a 'pathInfo' of */translate/speakeasy*. "
			+ "\\n"
			+ "# HTTPS Support and Sharing the Jetty Server\\n "
			+ "see also [namespace:com.ibm.streamsx.inet]";


	static final double DEFAULT_WEB_TIMEOUT = 15.0;
	static final String WEBTIMEOUT_DESC = "Number of seconds to wait for the web request to be processed by Streams, default: \\\"" + DEFAULT_WEB_TIMEOUT + "\\\".  ";

	// common to output/input
	static final String KEY_DESC = " Input and output port's corrolation key. The values is expected to be unchanged between the input and output, default: \\\"" + defaultKeyAttributeName + "\\\". ";
	// input port
	private static final String RESPONSEATTRNAME_DESC = "Input port's attribute name for the response body of the web response. The type of the attribute must be `rstring` or `ustring`. Default:  \\\"" + defaultResponseAttributeName + "\\\".  ";
	private static final String RESPONSEJSONATTRNAME_DESC = "Input port's attribute name for json results (complete response). The type of the attribute must be `rstring` or `ustring`. Default:  \\\"" + defaultJsonAttributeName + "\\\".  ";
	private static final String RESPONSESTATUSATTRNAME_DESC = "Input port's attribute attribute name for web status. The type of the attribute must be `int32`. Default:  \\\"" + defaultResponseStatusAttributeName + "\\\". "
			+ "In no web status is deliveres, the value: " + defaultStatusCode + " is used.";
	private static final String RESPONSECONTENTTYPE_DESC = "Input port's attribute name for web response content type. The type of the attribute must be `rstring` or `ustring`. Default: \\\"" + defaultContentTypeAttributeName + "\\\". "
			+ "If null or an empty string is delivered, the default content type '" +  defaultResponseContentType + "' is used.  ";
	private static final String RESPONSEHEADERATTRNAME_DESC = "Input port's attribute name for web response headers. The type of this attribute must be `map<rstring, rstring>`. Default: \\\"" + defaultHeaderAttributeName + "\\\".  ";
	// output port
	private static final String JSONATTRNAME_DESC = "Output port's attrinute name for json request (complete request). The type of the attribute must be `rstring` or `ustring`. Default:  \\\"" + defaultJsonAttributeName + "\\\".  ";
	private static final String REQUESTATTRNAME_DESC = "Output port's attribute name with the web request (body of the web request). The type of the attribute must be `rstring` or `ustring`. Default \\\"" + defaultRequestAttributeName + "\\\".  ";
	private static final String METHODATTRNAME_DESC = "Output ports's attribute name with the request method (PUT, GET, POST). The type of the attribute must be `rstring` or `ustring`. Default: \\\"" + defaultMethodAttributeName + "\\\".  ";
	private static final String PATHINFOATTRNAME_DESC = "Output ports's attribute name of the content path below the base. The type of the attribute must be `rstring` or `ustring`. Default \\\"" + defaultPathInfoAttributeName + "\\\".  ";
	private static final String CONTEXTPATHATTRNAME_DESC = "Output ports's attribute name of the context path. The type of the attribute must be `rstring` or `ustring`. Default: \\\"" + defaultContextPathAttributeName + "\\\".  ";
	private static final String URLATTRNAME_DESC = "Output ports's attribute name of the url. The type of the attribute must be `rstring` or `ustring`. Default \\\"" + defaultUrlAttributeName + "\\\".  ";
	private static final String CONTENTTYPEATTRNAME_DESC = "Output port's attribute name with content-type will be provided in. The type of the attribute must be `rstring` or `ustring`. Default: \\\"" + defaultContentTypeAttributeName + "\\\".  ";
	private static final String HEADERATTRNAME_DESC = "Output port's attribute name with web request headers, in the form of a `map<rstring, rstring>`, default: \\\"" + defaultHeaderAttributeName + "\\\".  ";

	/**
	 * Operator state
	 */
	private double webTimeout = DEFAULT_WEB_TIMEOUT;

	// input pot & output port
	private String keyAttributeName = defaultKeyAttributeName;
	
	// input port
	private String responseJsonAttributeName = defaultJsonAttributeName;
	private String responseAttributeName = null;
	private String responseStatusAttributeName = null;
	private String responseHeaderAttributeName = null;
	private String responseContentTypeAttributeName = null;

	//output port
	private String jsonAttributeName = defaultJsonAttributeName;
	private String requestAttributeName = null;
	private String methodAttributeName = null; // get/put/del/
	private String pathInfoAttributeName = null;
	private String contextPathAttributeName = null;
	private String urlAttributeName = null;
	private String headerAttributeName = null;
	private String contentTypeAttributeName = null;
	
	//internal state
	private boolean jsonFormatInPort = false ;   // only one column on input port jsonString
	private boolean jsonFormatOutPort = false;   // only one column on output port jsonString
	private Map<Long, ReqWebMessage> activeRequests = Collections.synchronizedMap(new HashMap<>());

	// Metrics
	private Metric nMessagesReceived;
	private Metric nMessagesResponded;
	private Metric nRequestTimeouts;
	private Metric nMissingTrackingKey;
	private Metric nTrackingKeyNotFound;
	private Metric nActiveRequests;
	
	/**
	 * Conduit object between operator and servlet.
	 * [0] - function to create output tuple.
	 * [1] - the web timeout
	 * [2] - the metric nRequestTimeouts
	 * [3] - the metric nActiveRequests
	 * [4] - the synchronized map with tracking-key AsyncContext association
	 */
	public static class RequestProcessConduit {
		final public Function<ReqWebMessage,OutputTuple> tupleCreator;
		final public double webTimeout;
		final public Metric nRequestTimeouts;
		final public Metric nActiveRequests;
		final public Map<Long, ReqWebMessage> activeRequests;
		
		public RequestProcessConduit(
				Function<ReqWebMessage,OutputTuple> tupleCreator, double webTimeout, 
				Metric nRequestTimeouts, Metric nActiveRequests, Map<Long, ReqWebMessage> activeRequests) {
			this.tupleCreator = tupleCreator;
			this.webTimeout = webTimeout;
			this.nRequestTimeouts = nRequestTimeouts;
			this.nActiveRequests = nActiveRequests;
			this.activeRequests = activeRequests;
		}
	}

	@Override
	protected Object getConduit() {
		return new RequestProcessConduit(this::initiateRequestFromWeb, webTimeout, nRequestTimeouts, nActiveRequests, activeRequests);
	}

	@Override
	protected String getSetupClass() {
		return com.ibm.streamsx.inet.rest.setup.RequestProcessSetup.class.getName();
	}

	/**
	 * Initialize this operator. Called once before any tuples are processed.
	 * 
	 * @param context
	 *            OperatorContext for this operator.
	 * @throws Exception
	 *             Operator failure, will cause the enclosing PE to terminate.
	 */
	@Override
	public synchronized void initialize(OperatorContext context) throws Exception {
		// Must call super.initialize(...) to correctly setup an operator.
		super.initialize(context);

		//detect json input/output format
		jsonFormatOutPort = (getOutput(0).getStreamSchema().getAttributeCount() == 1) && (jsonAttributeName.equals(getOutput(0).getStreamSchema().getAttributeNames().toArray()[0]));
		jsonFormatInPort = (getInput(0).getStreamSchema().getAttributeCount() == 1) && (responseJsonAttributeName.equals(getInput(0).getStreamSchema().getAttributeNames().toArray()[0]));
		
		//trace schema
		for (int idx = 0; getOutput(0).getStreamSchema().getAttributeCount() != idx; idx++) {
			trace.info(String.format(" -- OutPort@initalize attribute[%d]:%s ", idx, (getOutput(0).getStreamSchema().getAttributeNames().toArray()[idx])));
			System.out.printf(" -- OutPort@initalize attribute[%d]:%s \n", idx, (getOutput(0).getStreamSchema().getAttributeNames().toArray()[idx]));
		}
		for (int idx = 0; getInput(0).getStreamSchema().getAttributeCount() != idx; idx++) {
			trace.info(String.format(" -- InPort@initalize attribute[%d]:%s ", idx, (getInput(0).getStreamSchema().getAttributeNames().toArray()[idx])));
			System.out.printf(" -- InPort@initalize attribute[%d]:%s \n", idx, (getInput(0).getStreamSchema().getAttributeNames().toArray()[idx]));
		}
		
		//warn json but not single
		if ((getOutput(0).getStreamSchema().getAttributeCount() != 1) && (getOutput(0).getStreamSchema().getAttribute(jsonAttributeName) != null)) {
			trace.warn("found that '"+ jsonAttributeName+"' is not the only output port attribute, NOT using attribute.");
			System.out.println("WARNING: found that '"+ jsonAttributeName+"' is not the only output port attribute, NOT using attribute.");
		}
		if ((getInput(0).getStreamSchema().getAttributeCount() != 1) && (getInput(0).getStreamSchema().getAttribute(responseJsonAttributeName) != null)) {
			trace.warn("found that '"+ responseJsonAttributeName+"' is not the only input port attribute, NOT using attribute.");
			System.out.println("WARNING: found that '"+ responseJsonAttributeName+"' is not the only input port attribute, NOT using attribute.");
		}

		//Output attributes
		if (jsonFormatOutPort) {
			trace.info("Operator " + context.getName() + " single column output. Json output attribute: " + jsonAttributeName);
			System.out.println(RequestProcess.class.getName() + " Operator " + context.getName() + " single column output. json output attribute: " + jsonAttributeName);
		} else {
			// key, out port
			if (getOutput(0).getStreamSchema().getAttribute(keyAttributeName) == null)
				throw new IllegalArgumentException("Could not detect required attribute \"" + keyAttributeName + "\" on output port 0. "
						+ "Or specify a valid value for \"keyAttributeName\"");
			MetaType keyParamType = getOutput(0).getStreamSchema().getAttribute(keyAttributeName).getType().getMetaType();
			if (keyParamType != MetaType.INT64)
				throw new IllegalArgumentException("Only types \"" + MetaType.INT64 + "\" allowed for attribute \"" + keyAttributeName + "\"");
			// request, out port
			requestAttributeName     = checkOptionalStringAttribute(requestAttributeName,     defaultRequestAttributeName,     getOutput(0).getStreamSchema(), "output", "requestAttributeName");
			// header, out port
			headerAttributeName      = checkOptionalAttribute      (headerAttributeName,      defaultHeaderAttributeName,      getOutput(0).getStreamSchema(), MetaType.MAP, "output", "headerAttributeName");
			// method, out port
			methodAttributeName      = checkOptionalStringAttribute(methodAttributeName,      defaultMethodAttributeName,      getOutput(0).getStreamSchema(), "output", "methodAttributeName");
			// pathInfo, out port
			pathInfoAttributeName    = checkOptionalStringAttribute(pathInfoAttributeName,    defaultPathInfoAttributeName,    getOutput(0).getStreamSchema(), "output", "pathInfoAttributeName");
			// context path, out port
			contextPathAttributeName = checkOptionalStringAttribute(contextPathAttributeName, defaultContextPathAttributeName, getOutput(0).getStreamSchema(), "output", "contextPathAttributeName");
			// pathInfo, out port
			urlAttributeName         = checkOptionalStringAttribute(urlAttributeName,         defaultUrlAttributeName,         getOutput(0).getStreamSchema(), "output", "urlAttributeName");
			// contentType, out port
			contentTypeAttributeName = checkOptionalStringAttribute(contentTypeAttributeName, defaultContentTypeAttributeName, getOutput(0).getStreamSchema(), "output", "contentTypeAttributeName");
		}
		
		//input attributes
		if (jsonFormatInPort) {
			trace.trace("Operator " + context.getName() + " single column input. Json input attribute name: " + responseJsonAttributeName);
			System.out.println(RequestProcess.class.getName() + " Operator " + context.getName() + " single column input. Json input attribute name: " + responseJsonAttributeName);
		} else {
			// key, in port, mandatory
			if (getInput(0).getStreamSchema().getAttribute(keyAttributeName) == null)
				throw new IllegalArgumentException("Could not detect required attribute \"" + keyAttributeName + "\" on input port 0. "
					+ "Or specify a valid value for \"keyAttributeName\"");
			MetaType keyResponseParamType = getInput(0).getStreamSchema().getAttribute(keyAttributeName).getType().getMetaType();
			if (keyResponseParamType != MetaType.INT64)
				throw new IllegalArgumentException("Only types \"" + MetaType.INT64 + "\" allowed for attribute \"" + keyAttributeName + "\"");
			// response, in port, optional
			responseAttributeName            = checkOptionalStringAttribute(responseAttributeName,            defaultResponseAttributeName,       getInput(0).getStreamSchema(), "input", "responseAttributeName");
			// status, in port, optional
			responseStatusAttributeName      = checkOptionalAttribute      (responseStatusAttributeName,      defaultResponseStatusAttributeName, getInput(0).getStreamSchema(), MetaType.INT32, "input", "responseStatusAttributeName");
			// response headers, in port, optional
			responseHeaderAttributeName      = checkOptionalAttribute      (responseHeaderAttributeName,      defaultHeaderAttributeName,         getInput(0).getStreamSchema(), MetaType.MAP, "input", "responseHeaderAttributeName");
			// response content type, in port, optional
			responseContentTypeAttributeName = checkOptionalStringAttribute(responseContentTypeAttributeName, defaultContentTypeAttributeName,    getInput(0).getStreamSchema(), "input", "responseContentTypeAttributeName");
		}

		trace.trace("Operator " + context.getName() + " initializing in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId());
	}
	
	/**
	 * Check the existence and type of an optional stream attribute
	 */
	private static String checkOptionalAttribute(String attributeName, String defaultName, StreamSchema schema, MetaType attributeType, String inout, String paramName) {
		if (attributeName == null)
			if (schema.getAttribute(defaultName) != null)
				attributeName = defaultName;
		if (attributeName != null) {
			if (schema.getAttribute(attributeName) == null) {
				throw new IllegalArgumentException("Could not detect required attribute \"" + attributeName + "\" on " + inout + " port 0. "
						+ "Or specify a valid value for \"" + paramName + "\"");
			} else {
				MetaType mAttributeType = schema.getAttribute(attributeName).getType().getMetaType();
				if (mAttributeType != attributeType)
					throw new IllegalArgumentException("Only type of \"" + attributeType + "\" allowed for attribute \"" + attributeName + "\"");
			}
		}
		System.out.println("Attribute check " + inout + " port, parameter: " + paramName + " use attribute: "+ attributeName + " type: " + attributeType.name());
		return attributeName;
	}

	/**
	 * Check the existence and type of an optional stream string attribute
	 */
	private static String checkOptionalStringAttribute(String attributeName, String defaultName, StreamSchema schema, String inout, String paramName) {
		if (attributeName == null)
			if (schema.getAttribute(defaultName) != null)
				attributeName = defaultName;
		if (attributeName != null) {
			if (schema.getAttribute(attributeName) == null) {
				throw new IllegalArgumentException("Could not detect required attribute \"" + attributeName + "\" on " + inout + " port 0. "
						+ "Or specify a valid value for \"" + paramName + "\"");
			} else {
				MetaType mAttributeType = schema.getAttribute(attributeName).getType().getMetaType();
				if (mAttributeType != MetaType.USTRING && mAttributeType != MetaType.RSTRING)
					throw new IllegalArgumentException("Only types \"" + MetaType.USTRING + "\" and \"" + MetaType.RSTRING
							+ "\" allowed for attribute \"" + attributeName + "\"");
			}
		}
		System.out.println("Attribute check " + inout + " port, parameter: " + paramName + " use attribute: "+ attributeName + " type: string");
		return attributeName;
	}

	/**
	 * Setup the metrics
	 */
	@CustomMetric(description="Number of requests received from web.", kind=Kind.COUNTER)
	public void setnMessagesReceived(Metric nMessagesReceived) {
		this.nMessagesReceived = nMessagesReceived;
	}
	@CustomMetric(description="Number of vaild responses sent back via web.", kind=Kind.COUNTER)
	public void setnMessagesResponded(Metric nMessagesResponded) {
		this.nMessagesResponded = nMessagesResponded;
	}
	@CustomMetric(description="Missing tracking key count. In case of json input, this counts the received tuples with a corrupt"
			+ " json structure or the json structure does not contain a valid key object.", kind=Kind.COUNTER)
	public void setnMissingTrackingKey(Metric nMissingTrackingKey) {
		this.nMissingTrackingKey = nMissingTrackingKey;
	}
	@CustomMetric(description="Number of input tuples where the delivered tracking key was not found in the active messages database.", kind=Kind.COUNTER)
	public void setnTrackingKeyNotFound(Metric nTrackingKeyNotFound) {
		this.nTrackingKeyNotFound = nTrackingKeyNotFound;
	}
	@CustomMetric(description="Number of timeouts waiting for response from Streams.", kind=Kind.COUNTER)
	public void setnRequestTimeouts(Metric nRequestTimeouts) {
		this.nRequestTimeouts = nRequestTimeouts;
	}
	@CustomMetric(description="Number of requests currently being processed.", kind=Kind.GAUGE)
	public void setnActiveRequests(Metric nActiveRequests) {
		this.nActiveRequests = nActiveRequests;
	}

	/*
	 * Parameters
	 */
	@Parameter(optional = true, description = WEBTIMEOUT_DESC)
	public void setWebTimeout(double webTimeout) {
		this.webTimeout = webTimeout;
	}

	// OUTPUT - flow out of operator that that flows in from the web site
	@Parameter(optional = true, description = KEY_DESC)
	public void setKeyAttributeName(String keyAttributeName) {
		this.keyAttributeName = keyAttributeName;
	}

	@Parameter(optional = true, description = JSONATTRNAME_DESC)
	public void setJsonAttributeName(String jsonAttributeName) {
		this.jsonAttributeName = jsonAttributeName;
	}
	@Parameter(optional = true, description = METHODATTRNAME_DESC)
	public void setMethodAttributeName(String methodAttributeName) {
		this.methodAttributeName = methodAttributeName;
	}

	@Parameter(optional = true, description = PATHINFOATTRNAME_DESC)
	public void setPathInfoAttributeName(String pathInfoAttributeName) {
		this.pathInfoAttributeName = pathInfoAttributeName;
	}

	@Parameter(optional = true, description = CONTEXTPATHATTRNAME_DESC)
	public void setContextPathAttributeName(String contextPathAttributeName) {
		this.contextPathAttributeName = contextPathAttributeName;
	}

	@Parameter(optional = true, description = URLATTRNAME_DESC)
	public void setUrlAttributeName(String urlAttributeName) {
		this.urlAttributeName = urlAttributeName;
	}

	@Parameter(optional = true, description = REQUESTATTRNAME_DESC)
	public void setRequestAttributeName(String requestAttributeName) {
		this.requestAttributeName = requestAttributeName;
	}

	@Parameter(optional = true, description = HEADERATTRNAME_DESC)
	public void setHeaderAttributeName(String headerAttributeName) {
		this.headerAttributeName = headerAttributeName;
	}
	@Parameter(optional = true, description = CONTENTTYPEATTRNAME_DESC)
	public void setContentTypeAttributeName(String contentTypeAttributeName) {
		this.contentTypeAttributeName = contentTypeAttributeName;
	}

	// INPUT - flow into operator that is returned to the web requestor.
	@Parameter(optional = true, description = RESPONSEJSONATTRNAME_DESC)
	public void setResponseJsonAttributeName(String responseJsonAttributeName) {
		this.responseJsonAttributeName = responseJsonAttributeName;
	}
	@Parameter(optional = true, description = RESPONSEATTRNAME_DESC)
	public void setResponseAttributeName(String responseAttributeName) {
		this.responseAttributeName = responseAttributeName;
	}
	@Parameter(optional = true, description = RESPONSESTATUSATTRNAME_DESC)
	public void setResponseStatusAttributeName(String responseStatusAttributeName) {
		this.responseStatusAttributeName = responseStatusAttributeName;
	}

	@Parameter(optional = true, description = RESPONSEHEADERATTRNAME_DESC)
	public void setResponseHeaderAttributeName(String responseHeaderAttributeName) {
		this.responseHeaderAttributeName = responseHeaderAttributeName;
	}

	@Parameter(optional = true, description = RESPONSECONTENTTYPE_DESC)
	public void setResponseContentTypeAttributeName(String responseContentTypeAttributeName) {
		this.responseContentTypeAttributeName = responseContentTypeAttributeName;
	}

	/**
	 * The arriving tuple is used to build a web response.  The response is related to the output tuples that was generated
	 * by initateRequestFromTheWeb(). Based upon the tuples attributes received the response is built. 
	 * If the attribute is not present on the tuple a default is provided, except for the mandatory key. 
	 */
	@Override
	public final void process(StreamingInput<Tuple> inputStream, Tuple tuple) {

		if (trace.isEnabledFor(TraceLevel.DEBUG))
			trace.debug("processResponse ENTER");

		long trackingKey = 0;
		String response = "";
		long statusCode = defaultStatusCode;
		HashMap<String, String> responseHeaders = new HashMap<String, String>();
		String responseContentType = defaultResponseContentType;

		if (jsonFormatInPort) {
			if (trace.isEnabledFor(TraceLevel.TRACE))
				trace.trace("processResponse - DUMP JSON:" + tuple.toString());
			String jsonString = tuple.getString(responseJsonAttributeName);

			JSONObject json = null;
			try {
				json = JSONObject.parse(jsonString);
			} catch (IOException e) {
				// Handle a parse error - cannot send data via web since the key is in the jsonstring.
				nMissingTrackingKey.increment();
				trace.error("processResponse JSON - Failed  to parse json response string missingTrackingKeyCount: " + nMissingTrackingKey.getValue() + " jsonString:" + jsonString, e);
				return;
			}
			if (!json.containsKey(defaultKeyAttributeName)) {
				nMissingTrackingKey.increment();
				trace.error("processResponse JSON: Did not locate the element  '" + defaultKeyAttributeName + "' missingTrackingKeyCount: " + nMissingTrackingKey.getValue() + " in the JSON structure : " + jsonString);
				return;
			}
			try {
				trackingKey =  (long) json.get(defaultKeyAttributeName);
			} catch (ClassCastException e) {
				nMissingTrackingKey.increment();
				trace.error("processResponse JSON: ClassCastException for '" + defaultKeyAttributeName + "' missingTrackingKeyCount: " + nMissingTrackingKey.getValue() + " in the JSON structure : " + jsonString, e);
				return;
			}

			// Extract the components from json.
			try {
				if (json.containsKey(defaultResponseAttributeName)) {
					response = (String) json.get(defaultResponseAttributeName);
				}
				if (json.containsKey((String) defaultResponseStatusAttributeName)) {
					Long val = (Long) json.get(defaultResponseStatusAttributeName);
					statusCode = val;
				}
				if (json.containsKey((String) defaultHeaderAttributeName)) {
					@SuppressWarnings("unchecked")
					Map<String,String> mapHeader = (Map<String, String>) json.get(defaultHeaderAttributeName);
					mapHeader.forEach((key, value)->{ responseHeaders.put(key,value);});
					responseHeaders.forEach((key, value)->{ if (trace.isEnabledFor(TraceLevel.INFO)) trace.info("processResponse JSON : header key:" + key + " value: " + value);});
				}
				if (json.containsKey((String) defaultContentTypeAttributeName)) {
					String rct = (String) json.get(defaultContentTypeAttributeName);
					if ( ! rct.isEmpty() )
						responseContentType = rct;
				}
			} catch (ClassCastException e) {
				trace.error("processResponse JSON: ClassCastException for in the JSON structure : " + jsonString, e);
				handleProcessError(trackingKey, HttpServletResponse.SC_BAD_REQUEST, "ClassCastException in the JSON structure");
			}
		} else {
			if (trace.isEnabledFor(TraceLevel.INFO))
				trace.info("processResponse TUPLE");

			// Extract components for response....
			trackingKey = tuple.getLong(keyAttributeName);
			if (responseAttributeName != null)
				response = tuple.getString(responseAttributeName);
			if (responseStatusAttributeName != null)
				statusCode = tuple.getInt(responseStatusAttributeName);
			if (responseHeaderAttributeName != null) {
				@SuppressWarnings("unchecked")
				Map<RString, RString> mapHeader = (Map<RString, RString>) tuple.getMap(responseHeaderAttributeName);
				for (Iterator<RString> keys = mapHeader.keySet().iterator(); keys.hasNext();) {
					RString key = keys.next();
					responseHeaders.put(key.getString(), mapHeader.get(key).getString());
				}
				responseHeaders.forEach((key, value)->{ if (trace.isEnabledFor(TraceLevel.TRACE)) trace.trace("processResponse Tuple : header key:" + key + " value: " + value);});
			}
			if (responseContentTypeAttributeName != null) {
				String rct = tuple.getString(responseContentTypeAttributeName);
				if ( ! rct.isEmpty() )
					responseContentType = rct;
			}
		}

		ReqWebMessage activeWebMessage = activeRequests.remove(trackingKey);
		nActiveRequests.setValue(activeRequests.size());
		if (activeWebMessage == null) {
			nTrackingKeyNotFound.increment();
			trace.error("Tracking key: " + trackingKey + " not found in activeMessages");
			return;
		}

		
		try {
			InjectWithResponse.buildWebResponse(activeWebMessage, response, (int)statusCode, responseHeaders, responseContentType);
			nMessagesResponded.incrementValue(1L);
		} catch (Exception e) {
			trace.error(e.getClass().getName() + " " + e.getMessage() + " trackingKey: " + trackingKey, e);
			InjectWithResponse.buildWebErrResponse(activeWebMessage, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getClass().getName() + ": " + e.getMessage());
		}
		if (trace.isEnabledFor(TraceLevel.DEBUG))
			trace.debug("processResponse EXIT response : trackingKey:" + trackingKey);
	}

	/**
	 * Handles an error and the tracking key is known
	 * Can not build web response with unknown tracking key
	 * @param trackingKey
	 * @param statusCode
	 * @param statusMessage
	 */
	private void handleProcessError(long trackingKey, int statusCode, String statusMessage) {
		if (trace.isEnabledFor(TraceLevel.DEBUG))
			trace.debug("handleProcessError trackingKey: " + trackingKey + " statusCode: " + statusCode + " statusMessage: " + statusMessage);
		ReqWebMessage activeWebMessage = activeRequests.remove(trackingKey);
		nActiveRequests.setValue(activeRequests.size());
		if (activeWebMessage == null) {
			nTrackingKeyNotFound.increment();
			trace.error("Tracking key: " + trackingKey + " not found in activeMessages; webError");
			return;
		}
		
		InjectWithResponse.buildWebErrResponse(activeWebMessage, statusCode, statusMessage);
	}
	
	/**
	 * A web request will arrive from the web and be injected into the Stream. 
	 * A request is injected into the stream response will enter through
	 * the process() method above. 
	 */
	@SuppressWarnings("unchecked")
	public OutputTuple initiateRequestFromWeb(ReqWebMessage exchangeWebMessage) {

		if (trace.isEnabledFor(TraceLevel.INFO))
				trace.info("initiateWebRequest ENTER # " + nMessagesReceived.getValue() +" trackingKey: " + exchangeWebMessage.getTrackingKey());
		nMessagesReceived.increment();
		activeRequests.put(exchangeWebMessage.getTrackingKey(), exchangeWebMessage);
		nActiveRequests.setValue(activeRequests.size());

		HttpServletRequest request = (HttpServletRequest)exchangeWebMessage.getAsyncContext().getRequest();
		StringBuffer readerSb = new StringBuffer();
		try {
			Stream<String> stream = request.getReader().lines();
			stream.forEach(item -> readerSb.append(item));
			stream.close();
		} catch (IOException e) {
			trace.error("Error reading request of context path : " + request.getContextPath(), e);
		}
		String queryString = request.getQueryString();

		final String requestPayload;
		if (queryString != null) {
			requestPayload = queryString;
		} else {
			requestPayload = readerSb.toString();
		}
		
		Map<String, String> headers = new Hashtable<String, String>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			headers.put(name, request.getHeader(name));
		}
		
		StreamingOutput<OutputTuple> outStream = getOutput(0);
		OutputTuple outTuple = outStream.newTuple();

		if (jsonFormatOutPort) {
			JSONObject jsonObj = new JSONObject();

			jsonObj.put(RequestProcess.defaultKeyAttributeName, exchangeWebMessage.getTrackingKey());
			jsonObj.put(RequestProcess.defaultRequestAttributeName, requestPayload);
			jsonObj.put(RequestProcess.defaultMethodAttributeName, request.getMethod());
			jsonObj.put(RequestProcess.defaultContentTypeAttributeName, request.getContentType());
			jsonObj.put(RequestProcess.defaultContextPathAttributeName,  request.getContextPath());
			jsonObj.put(RequestProcess.defaultPathInfoAttributeName, request.getPathInfo());
			jsonObj.put(RequestProcess.defaultUrlAttributeName, request.getRequestURL().toString());
			if (JSONObject.isValidObject(headers)) {
				jsonObj.put(RequestProcess.defaultHeaderAttributeName, headers);
			} else {
				//Invalid for JSON (Hashtable), switch over to jsonObject
				JSONObject jsonHead = new JSONObject();
				jsonHead.putAll(headers);
				jsonObj.put(RequestProcess.defaultHeaderAttributeName, jsonHead);
			}

			String jsonRequestString = jsonObj.toString();
			outTuple.setString(jsonAttributeName, jsonRequestString);
		} else {
			outTuple.setLong(keyAttributeName, exchangeWebMessage.getTrackingKey());
			if (requestAttributeName != null) {
				outTuple.setString(requestAttributeName, requestPayload);
			}
			if (methodAttributeName != null) {
				outTuple.setString(methodAttributeName, request.getMethod());
			}
			if (pathInfoAttributeName != null) {
				String pi = "";
				if (request.getPathInfo() != null)
					pi = request.getPathInfo();
				outTuple.setString(pathInfoAttributeName, pi);
			}
			if (contentTypeAttributeName != null) {
				String pi = "";
				if (request.getContentType() != null)
					pi = request.getContentType();
				outTuple.setString(contentTypeAttributeName, pi);
			}
			if (urlAttributeName != null) {
				String pi = "";
				if (request.getRequestURL() != null)
					pi = request.getRequestURL().toString();
				outTuple.setString(urlAttributeName, pi);
			}
			if (contentTypeAttributeName != null) {
				String ct = "";
				if (request.getContentType() != null)
					ct = request.getContentType();
				outTuple.setString(contentTypeAttributeName, ct);
			}
			if (headerAttributeName != null) {
				HashMap<RString, RString> transfer = new HashMap<RString, RString>();
				for (Iterator<String> keys = headers.keySet().iterator(); keys.hasNext();) {
					String key = (String) keys.next();
					transfer.put(new RString(key), new RString(headers.get(key)));
				}
				outTuple.setMap(headerAttributeName, transfer);
			}

			if (trace.isEnabledFor(TraceLevel.INFO))
				trace.info("initiateWebRequest EXIT ");
		}
		return outTuple;
	}

}
