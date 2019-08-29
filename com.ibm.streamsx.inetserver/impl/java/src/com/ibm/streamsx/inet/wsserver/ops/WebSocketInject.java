package com.ibm.streamsx.inet.wsserver.ops;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import com.ibm.streams.operator.Attribute;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.OperatorContext.ContextCheck;
import com.ibm.streams.operator.Type.MetaType;
import com.ibm.streams.operator.compile.OperatorContextChecker;
import com.ibm.streams.operator.log4j.TraceLevel;
import com.ibm.streams.operator.metrics.Metric;
import com.ibm.streams.operator.metrics.Metric.Kind;
import com.ibm.streams.operator.model.CustomMetric;
import com.ibm.streams.operator.model.Icons;
import com.ibm.streams.operator.model.OutputPortSet;
import com.ibm.streams.operator.model.OutputPorts;
import com.ibm.streams.operator.model.Parameter;
import com.ibm.streams.operator.model.PrimitiveOperator;
import com.ibm.streams.operator.types.ValueFactory;
import com.ibm.streams.operator.model.OutputPortSet.WindowPunctuationOutputMode;
import com.ibm.streams.operator.state.ConsistentRegionContext;
import com.ibm.streamsx.inet.messages.Messages;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.wsserver.servlet.EventSocket;
import com.ibm.streamsx.inet.wsserver.servlet.EventSocketConduit;
import com.ibm.streamsx.inet.wsserver.servlet.InjectEventSocket;

/**
 * A source type operator that receives websockets messages from multiple clients. 
 * Each message arriving on the WebSocket is converted to a tuple and injected into 
 * the stream.  
 * 
 * Optionally the tuple being injected can include and identifier of the sender,
 * the identifier is unique for the lifetime of the session. 
 *  
 */
@PrimitiveOperator(description=WebSocketInject.OPERATOR_DESCR)
@OutputPorts({@OutputPortSet(description=WebSocketInject.OUTPUT_PORT_DESCR, cardinality=1, optional=false, windowPunctuationOutputMode=WindowPunctuationOutputMode.Free)})
@Icons(location32="icons/WebSocketInject_32.gif", location16="icons/WebSocketInject_16.gif")
public class WebSocketInject extends ServletOperator {

	static final String OPERATOR_DESCR = 
			" Operator recieves messages from WebSocket clients and generates a tuple which is sent to streams. "
			+ "Upon startup, this operator is registered to the common pe-wide jetty web server. "
			+ "Clients may connect to the websocket URL **/output/0/wsinject**. "
			+ " Each received message is output as tuple. The data received is dependent upon "
			+ " the input ports schema.\\n"
			+ "# HTTPS Support and Sharing the Jetty Server\\n "
			+ "see also [namespace:com.ibm.streamsx.inet]";

	static final String OUTPUT_PORT_DESCR = 
			"This output port injects the received web socket messages into the Streams application. If the port has one "
			+ "attribute, this attribute is used for the web socket messages and must be of type `rstring`, `ustring` or `blob`. If the port "
			+ "has more than one attribute, the output mapping is determined with parameters `messageAttributeName` and "
			+ "`senderIdAttributeName`. "
			+ "Subsequent attribute(s) are allowed but will not be poplulated.";

	static final String MESSAGE_ATTRIBUTE_NAME_DESCR = 
			"Output port's attribute that the data received will be stored to. The type of this attribute must be `rstring`, `ustring` or `blob`. "
			+ "If the output port has more than one attribute this parameter is required.";

	static final String SENDER_ID_ATTRIBUTE_NAME_DESCR = 
			"Output port attribute that will we loaded with the message sender's identifier, this "
			+ "identifier is consistent during the lifetime of the sender's session. "
			+ "The type of this attribute must be `rstring` or `ustring`.";

	static final String ACK_COUNT_DESCR = 
			"The operator sends out an ack message to all currently connected clients.  " +
			"An ack message is sent when the (totaslNumberOfMessagesRecieved % ackCount) == 0, " +
			"The ack message is a in JSON format " + "\\\\{" + " status:'COUNT', text:<totalNumberOfMessagesReceived>" + "\\\\}. " +
			"Default value is 0, no ack messages will be sent.";

	static final String ENABLE_CONNECTION_CONTROL_MESSAGES_DESCR = 
			"If this parameteter is true, the operator sends out a connection control message to "
			+ "all connected websocket clients, when a client gets connected or disconnected. Default is true.";

	/*
	 * Operator / Class Attributes
	 */
	private static final Logger trace = Logger.getLogger(WebSocketInject.class.getName());

	private String messageAttributeName = null;
	private String senderIdAttributeName = null;
	private int ackCount = 0;
	private boolean enableConnectionControlMessages = true;

	private Metric nClientsConnected;
	private Metric nMessagesReceived;

	private boolean binaryMessageMode = false;

	private final Map<String, Session> sessionsConnected = Collections.synchronizedMap(new HashMap<String, Session>());
	
	private ScheduledExecutorService scheduler;
	
	/*
	 * Parameters and Metrics
	 */
	@Parameter(optional=true,description=MESSAGE_ATTRIBUTE_NAME_DESCR)
	public void setMessageAttributeName(String messageAttributeName) {
		this.messageAttributeName = messageAttributeName;
	}
	@Parameter(optional=true,description=SENDER_ID_ATTRIBUTE_NAME_DESCR)
	public void setSenderIdAttributeName(String senderIdAttributeName) {
		this.senderIdAttributeName = senderIdAttributeName;
	}
	@Parameter(optional=true,description=ACK_COUNT_DESCR)
	public void setAckCount(int ackCount) {
		this.ackCount = ackCount;
	}
	@Parameter(optional=true, description=ENABLE_CONNECTION_CONTROL_MESSAGES_DESCR)
	public void setEnableConnectionControlMessages(boolean enableConnectionControlMessages) {
		this.enableConnectionControlMessages = enableConnectionControlMessages;
	}

	@CustomMetric(description=EventSocket.N_CLIENTS_CONNRCTED_DESCR, kind=Kind.GAUGE)
	public void setnClientsConnected(Metric nClientsConnected) {
		this.nClientsConnected = nClientsConnected;
	}

	@CustomMetric(description=InjectEventSocket.N_MESSAGES_RECEIVED_DESCR, kind=Kind.COUNTER)
	public void setnMessagesReceived(Metric nMessagesReceived) {
		this.nMessagesReceived = nMessagesReceived;
	}

	/*
	 * Compile time checks
	 */
	@ContextCheck(compile = true)
	public static void checkInConsistentRegion(OperatorContextChecker checker) {
		ConsistentRegionContext consistentRegionContext = checker.getOperatorContext().getOptionalContext(
				ConsistentRegionContext.class);
		if(consistentRegionContext != null) {
			checker.setInvalidContext(Messages.getString("CONSISTENT_CHECK_2"), new String[] {WebSocketSend.class.getName()});
		}
	}

	@ContextCheck(compile = true)
	public static void checkMethodParams(OperatorContextChecker occ) {
		int attributeCount = occ.getOperatorContext().getStreamingOutputs().get(0).getStreamSchema().getAttributeCount();
		boolean hasParamMessageAttributeName = occ.getOperatorContext().getParameterNames().contains("messageAttributeName");
		if ((attributeCount > 1) && ! hasParamMessageAttributeName) {
			occ.setInvalidContext("Parameter messageAttributeName is required if output port has more than one attribute", null);
		}
	}

	/*
	 * Operator Logic
	 */
	@Override
	protected Object getConduit() {
		return new EventSocketConduit(
				this::produceTuple, sessionsConnected, nClientsConnected,
				nMessagesReceived, ackCount, enableConnectionControlMessages);
	}

	@Override
	public String getSetupClass() {
		return com.ibm.streamsx.inet.wsserver.setup.WebSocketInjectSetup.class.getName();
	}

	@Override
	public synchronized void initialize(OperatorContext context) throws Exception {
		// Must call super.initialize(...) to correctly setup an operator.
		super.initialize(context);
		
		scheduler = context.getScheduledExecutorService();
		
		//if number of out attributes is 1 and no messageAttributeName is given -> set it
		if (getOutput(0).getStreamSchema().getAttributeCount() == 1) {
			if (messageAttributeName == null) {
				messageAttributeName = getOutput(0).getStreamSchema().getAttribute(0).getName();
			}
		}
		
		//check required parameters
		if (messageAttributeName == null) {
			throw new IllegalArgumentException("Parameter messageAttributeName is required if output port has more than one attribute");
		}
		
		//check if attribute names are there and type is correct
		Attribute messageAttribute = getOutput(0).getStreamSchema().getAttribute(messageAttributeName);
		if (messageAttribute == null) {
			throw new IllegalArgumentException("Could not detect required attribute '" + messageAttributeName + "' on output port 0. ");
		} else {
			MetaType attrType = messageAttribute.getType().getMetaType();
			if ((attrType == MetaType.RSTRING) || (attrType == MetaType.USTRING)) {
				binaryMessageMode = false;
			} else if (attrType == MetaType.BLOB) {
				binaryMessageMode = true;
			} else {
				throw new IllegalArgumentException("Port Attribute " + messageAttributeName + " must be of type rstring, ustring or blob");
			}
		}
		if (senderIdAttributeName != null) {
			Attribute senderIdAttribute = getOutput(0).getStreamSchema().getAttribute(senderIdAttributeName);
			if (senderIdAttribute == null) {
				throw new IllegalArgumentException("Could not detect required attribute '" + senderIdAttributeName + "' on output port 0. ");
			} else {
				MetaType attrType = senderIdAttribute.getType().getMetaType();
				if ((attrType != MetaType.RSTRING) || (attrType == MetaType.USTRING))
					throw new IllegalArgumentException("Port Attribute " + senderIdAttributeName + " must be of type rstring or ustring");
			}
		}
	}

	/**
	 * Message and Session information class used in tupleCreator
	 * Use either the message parameter for Text messages
	 * or payload, offset and len for binary messages
	 */
	public static class WebMessageInfo {
		final public String message;
		final byte[] payload;
		final int offset;
		final int len;
		final public String id;
		public WebMessageInfo(String message, byte[] payload, int offset, int len, String id) {
			this.message = message;
			this.payload = payload;
			this.offset = offset;
			this.len = len;
			this.id = id;
		}
	}

	public void produceTuple(WebMessageInfo webMessageInfo) {

		if (trace.isEnabledFor(TraceLevel.INFO)) trace.info("Enter initiateRequestFromWebid: " + webMessageInfo.id);
		
		StreamingOutput<OutputTuple> outStream = getOutput(0);
		OutputTuple outTuple = outStream.newTuple();

		if (binaryMessageMode) {
			if (webMessageInfo.message != null) {
				trace.error("Binary websocket messages are expected, but text message is received. Id: " + webMessageInfo.id);
			} else {
				outTuple.setBlob(messageAttributeName, ValueFactory.newBlob(webMessageInfo.payload, webMessageInfo.offset, webMessageInfo.len));
			}
		} else {
			if (webMessageInfo.payload != null) {
				trace.error("Text websocket messages are expected, but binary message is received. Id: " + webMessageInfo.id);
			} else {
				outTuple.setString(messageAttributeName, webMessageInfo.message);
			}
		}

		if (senderIdAttributeName != null) {
			outTuple.setString(senderIdAttributeName, webMessageInfo.id);
		}

		try {
			scheduler.submit(outStream.deferredSubmit(outTuple));
		} catch (Exception e) {
			trace.error("Failed to submit tuple - msg:" + e.getMessage(), e);
		}
	}
	
}
