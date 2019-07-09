package com.ibm.streamsx.inet.wsserver.ops;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.streams.operator.Attribute;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streams.operator.TupleAttribute;
import com.ibm.streams.operator.Type.MetaType;
import com.ibm.streams.operator.OperatorContext.ContextCheck;
import com.ibm.streams.operator.compile.OperatorContextChecker;
import com.ibm.streams.operator.encoding.EncodingFactory;
import com.ibm.streams.operator.encoding.JSONEncoding;
import com.ibm.streams.operator.metrics.Metric;
import com.ibm.streams.operator.metrics.Metric.Kind;
import com.ibm.streams.operator.model.CustomMetric;
import com.ibm.streams.operator.model.Icons;
import com.ibm.streams.operator.model.InputPortSet;
import com.ibm.streams.operator.model.InputPorts;
import com.ibm.streams.operator.model.Parameter;
import com.ibm.streams.operator.model.PrimitiveOperator;
import com.ibm.streams.operator.types.Blob;
import com.ibm.streams.operator.model.InputPortSet.WindowMode;
import com.ibm.streams.operator.state.ConsistentRegionContext;
import com.ibm.streamsx.inet.messages.Messages;
import com.ibm.streamsx.inet.rest.ops.ServletOperator;
import com.ibm.streamsx.inet.wsserver.servlet.EventSocket;
import com.ibm.streamsx.inet.wsserver.servlet.EventSocketConduit;

/**
 * A Sink class operator that is connected to multiple websocket clients, 
 * messages received on the input port are sent out as json messages. 
 * 
 * Clients can drop and connect at anytime. Message are only sent to those
 * that are connected at the time that the tuple arrives on the input port.  
 * 
 */
@PrimitiveOperator(description=WebSocketSend.OPERATOR_DESCR)
@InputPorts({@InputPortSet(description=WebSocketSend.INPUT_PORT_DESCR, cardinality=1, optional=false, windowingMode=WindowMode.NonWindowed)})
@Icons(location32="icons/WebSocketSend_32.gif", location16="icons/WebSocketSend_16.gif")
public class WebSocketSend extends ServletOperator {

	static final String OPERATOR_DESCR =
			"Operator transmits tuples recieved on the input port via WebSocket protocol to connected clients. "
			+ "Upon startup, this operator is registered to the common pe-wide jetty web server. "
			+ "Clients may connect to the websocket context under /input/0/wssend. "
			+ "As tuple arrives on the input port a message is triggered and transmitted to all currently connected clients. "
			+ "Clients can connect and disconnect at anytime.\\n"
			+ "# HTTPS Support and Sharing the Jetty Server\\n "
			+ "see also [namespace:com.ibm.streamsx.inet]";

	static final String INPUT_PORT_DESCR =
			"Input port: Tuples received on this port generate a message to be transmitted over to "
			+ "all connected websocket clients. The content of the transmitted message is determined with parameter "
			+ "`textMessage` or `binaryMessageAttributeName`. If both parameters are absend, the input tuple is converted "
			+ " into a JSON formatted messages and transmitted to all currently connected clients";

	private static final String ENABLE_CONNECTION_CONTROL_MESSAGES_DESCR =
			"If this parameteter is true, the operator sends out a connection control message to "
			+ "all connected websocket clients, when a client gets connected or disconnected. Default is true.";

	private static final String TEXT_MESSAGE_DESCR =
			"The input attribute which is used as text message. The attribute must be of type `rstring` or `ustring`. "
			+ "If this parameter is set, parameter `binaryMessageAttributeName` is not allowed.";

	private static final String BINARY_MESSAGE_ATTRIBUTE_NAME_DESCR =
			"The input attribute name which is used as binary message. The type of this attribute must be `blob`. "
			+ "If this parameter is set, parameter `textMessage` is not allowed.";

	/*
	 * Operator / Class attributes
	 */
	private static final Logger trace = Logger.getLogger(WebSocketSend.class.getName());
	
	private boolean enableConnectionControlMessages = true;
	private TupleAttribute<Tuple, String> textMessage = null;
	private String binaryMessageAttributeName = null;
	
	private Metric nMessagesSent;
	private Metric nClientsConnected;
	
	private final Map<Long, Session> sessionsConnected = Collections.synchronizedMap(new HashMap<Long, Session>());
	private AtomicLong messagesSent = new AtomicLong();

	/*
	 * Parameters and Metrics
	 */
	@Parameter(optional=true, description=TEXT_MESSAGE_DESCR)
	public void setTextMessage(TupleAttribute<Tuple, String> textMessage) {
		this.textMessage = textMessage;
	}
	@Parameter(optional=true, description=BINARY_MESSAGE_ATTRIBUTE_NAME_DESCR)
	public void setBinaryMessageAttributeName(String binaryMessageAttributeName) {
		this.binaryMessageAttributeName = binaryMessageAttributeName;
	}
	@Parameter(optional=true, description=ENABLE_CONNECTION_CONTROL_MESSAGES_DESCR)
	public void setEnableConnectionControlMessages(boolean enableConnectionControlMessages) {
		this.enableConnectionControlMessages = enableConnectionControlMessages;
	}

	@CustomMetric(description="Number of messages sent using WebSocket (cumulative sum for all connected clients)", kind=Kind.COUNTER)
	public void setnMessagesSent(Metric nMessagesSent) {
		this.nMessagesSent = nMessagesSent;
	}
	@CustomMetric(description=EventSocket.N_CLIENTS_CONNRCTED_DESCR, kind=Kind.GAUGE)
	public void setnClientsConnected(Metric nClientsConnected) {
		this.nClientsConnected = nClientsConnected;
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
		occ.checkExcludedParameters("textMessage", "binaryMessageAttributeName");
	}

	/*
	 * Operator logic
	 */
	@Override
	protected Object getConduit() {
		return new EventSocketConduit(null, sessionsConnected, nClientsConnected, null, 0, enableConnectionControlMessages);
	}

	@Override
	protected String getSetupClass() {
		return com.ibm.streamsx.inet.wsserver.setup.WebSocketSendSetup.class.getName();
	}

	@Override
	public synchronized void initialize(OperatorContext context) throws Exception {
		super.initialize(context);
		
		if (binaryMessageAttributeName != null) {
			Attribute binaryMessageAttribute = getInput(0).getStreamSchema().getAttribute(binaryMessageAttributeName);
			if ( binaryMessageAttribute == null) {
				throw new IllegalArgumentException("Could not detect required attribute \"" + binaryMessageAttributeName + "\" on input port 0. "
						+ "Or specify a valid value for \"binaryMessageAttributeName\"");
			} else {
				if (getInput(0).getStreamSchema().getAttribute(binaryMessageAttributeName).getType().getMetaType() != MetaType.BLOB) {
					throw new IllegalArgumentException(Messages.getString("PARAM_ATTRIBUTE_TYPE_CHECK_1", MetaType.BLOB, binaryMessageAttributeName));
				}
			}
		}
	}
	
	@Override
	public final void process(StreamingInput<Tuple> inputStream, Tuple tuple) {
		if (trace.isTraceEnabled()) trace.trace("ENTER process");
		
		boolean binaryMessageMode = false;
		String messageString = null;
		Blob messageBlob = null;
		if ((textMessage == null) && (binaryMessageAttributeName == null)) {
			JSONEncoding<JSONObject, JSONArray> jsonEncoding = EncodingFactory.getJSONEncoding();
			JSONObject jobject = new JSONObject();
			jobject.put("tuple", jsonEncoding.encodeTuple(tuple));
			messageString = jobject.toString();
		} else {
			if (textMessage != null) {
				messageString = textMessage.getValue(tuple);
			} else {
				messageBlob = tuple.getBlob(binaryMessageAttributeName);
				binaryMessageMode = true;
			}
		}
		synchronized (sessionsConnected) {
			Collection<Session> sessions = sessionsConnected.values();
			for (Session session : sessions) {
				if (session.isOpen()) {
					try {
						if (binaryMessageMode) {
							session.getRemote().sendBytes(messageBlob.getByteBuffer());
						} else {
							session.getRemote().sendString(messageString);
						}
						messagesSent.getAndIncrement();
					} catch (Exception e) {
						trace.error("Can not send ws message to " + session.getRemoteAddress().getHostString() 
								+ ":" + session.getRemoteAddress().getPort() + " error: " + e.getClass().getName()
								+ " " + e.getMessage(), e);
					}
				} else {
					if (trace.isInfoEnabled()) trace.info("Session already closed " + session.getRemoteAddress().getHostString()
							+ ":" + session.getRemoteAddress().getPort());
				}
			}
		}
		
		nMessagesSent.setValue(messagesSent.get());
	}

}
