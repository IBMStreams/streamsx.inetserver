/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2011, 2014 
*/
package com.ibm.streamsx.inet.rest.ops;

import java.util.ArrayList;
import java.util.List;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streams.operator.OperatorContext.ContextCheck;
import com.ibm.streams.operator.compile.OperatorContextChecker;
import com.ibm.streams.operator.model.Icons;
import com.ibm.streams.operator.model.InputPortSet;
import com.ibm.streams.operator.model.InputPortSet.WindowMode;
import com.ibm.streams.operator.model.InputPortSet.WindowPunctuationInputMode;
import com.ibm.streams.operator.model.InputPorts;
import com.ibm.streams.operator.model.Parameter;
import com.ibm.streams.operator.model.PrimitiveOperator;

@PrimitiveOperator(name=TupleView.opName, description=TupleView.DESC)
// Require at least one input port
@InputPorts({
	@InputPortSet(cardinality=1,windowingMode=WindowMode.Windowed,windowPunctuationInputMode=WindowPunctuationInputMode.WindowBound,
			description="Windowed input port whose tuples will be available using a HTTP GET request with a URL using port index 0."),
	@InputPortSet(optional=true,windowingMode=WindowMode.Windowed,windowPunctuationInputMode=WindowPunctuationInputMode.WindowBound,
			description="Optional windowed input ports whose tuples will be available using a HTTP GET request a URL with the corresponding port index.")
	})
@Icons(location32="icons/"+TupleView.opName+"_32.gif", location16="icons/"+TupleView.opName+"_16.gif")

public class TupleView extends ServletOperator {
	
	static final String opName = "HTTPTupleView";
	
	//parameter values
	private ArrayList<String>  partitionKey = null;
	private ArrayList<String>  partitionBy = null;
	private boolean            namedPartitionQuery = false;
	//obtained properties
	private boolean                       anyInputIsPartitioned   = false;
	private ArrayList<ArrayList<String>>  partitonAttributeNames  = new ArrayList<ArrayList<String>>();
	private ArrayList<ArrayList<Integer>> partitonAttributeIndexes= new ArrayList<ArrayList<Integer>>();
	private ArrayList<Boolean>            attributeIsPartitionKey = new ArrayList<Boolean>();
	private ArrayList<Boolean>            suppressIsPartitionKey  = new ArrayList<Boolean>();
	private ArrayList<Boolean>            callbackIsPartitionKey  = new ArrayList<Boolean>();
	
	
	@Parameter(optional=true, cardinality=-1, description="Names of attributes to partition the window by. If the cardinality of this parameter is > 1, "
			+ "then every value represents one attribute name. If the cadinality equals to 1, the value may contain one attribute name or a comma separated list of attribute names. "
			+ "The values of this parameter list are applied to the window partitions of all input ports. Thus the input ports must have equal port schemata. If different "
			+ "partition lists are necessary use parameter `partitionBy`. Empty values are omitted. This parameter must not be used with parameter `partitionBy`.")
	public void setPartitionKey(List<String> val) {
		if (val.size() == 1) {
			String[] stringArr = val.get(0).split(",");
			partitionKey = new ArrayList<String>();
			for (int i = 0; i < stringArr.length; i++) {
				if ( ! stringArr[i].isEmpty())
					partitionKey.add(stringArr[i]);
			}
		} else {
			partitionKey = new ArrayList<String>();
			for (int i = 0; i < val.size(); i++)
				if ( ! val.get(i).isEmpty())
					partitionKey.add(val.get(i));
		}
	}

	@Parameter(optional=true, cardinality=-1, description="Names of attributes to partition the window by. The cardinality of this parameter must be equal the number of input ports. "
			+ "Each value must contain one a comma separated list of the partition attributes of the corresponding input port. "
			+ "If an input port is not partitioned, the corresponding value must be the empty string. "
			+ "Thus the input ports may have different port schemata. This parameter must not be used with parameter `partitionKey`.")
	public void setPartitionBy(List<String> partitionBy) {
		this.partitionBy =  new ArrayList<String>(partitionBy);
	}
	
	@Parameter(optional=true, description="")
	public void setNamedPartitionQuery(boolean namedPartitionQuery) {
		this.namedPartitionQuery = namedPartitionQuery;
	}

	// Parameter setters just to define the parameters in the SPL operator model.
	@Parameter(optional = true, description = "List of headers to insert into the http reply. Formatted as header:value")
	public void setHeaders(String[] headers) {}

	@ContextCheck(compile = true)
	public static void checkParams(OperatorContextChecker occ) {
		occ.checkExcludedParameters("partitionKey", "partitionBy");
	}

	@Override
	public String getSetupClass() {
		return com.ibm.streamsx.inet.rest.setup.TupleViewSetup.class.getName();
	}

	@Override
	public void initialize(OperatorContext context) throws Exception {
		
		//initialize partitonAttributeNames and partitonAttributeIndexes
		for (StreamingInput<Tuple> port : context.getStreamingInputs()) {
			partitonAttributeNames.add(new ArrayList<String>());
			partitonAttributeIndexes.add(new ArrayList<Integer>());
			attributeIsPartitionKey.add(new Boolean(false));
			suppressIsPartitionKey.add(new Boolean(false));
			callbackIsPartitionKey.add(new Boolean(false));
			if (port.getStreamWindow().isPartitioned())
				anyInputIsPartitioned = true;
		}
		System.out.println("namedPartitionQuery:   " + Boolean.toString(namedPartitionQuery));
		System.out.println("anyInputIsPartitioned: " + Boolean.toString(anyInputIsPartitioned));
		
		//if no input is partitioned partitionKey and partitionBy must be null or completely empty
		if ( ! anyInputIsPartitioned ) {
			if ((partitionKey != null) && (partitionKey.size() > 0) && ! partitionKey.get(0).isEmpty())
				throw new IllegalArgumentException("No Input port window is partitioned, but parameter partitionKey has a non empty value");
			if (partitionBy != null) {
				for (int i = 0; i < partitionBy.size(); i++) {
					if ( ! partitionBy.get(i).isEmpty())
						throw new IllegalArgumentException("No Input port window is partitioned, but parameter partitionBy has non empty value");
				}
			}
		} else {
		
			//move values from parameters "partitionKey" and "partitionBy" into ArrayList<ArrayList<String>> partitonAttributeNames
			final int numberInputPorts = context.getStreamingInputs().size();
			for (int i = 0; i < numberInputPorts; i++) {
				ArrayList<String> portPartitonAttributeNames = partitonAttributeNames.get(i);
				if (partitionKey != null) {
					for (int j = 0; j < partitionKey.size(); j++) { //empty values are already skipped
						portPartitonAttributeNames.add(partitionKey.get(j));
					}
				}
				if (partitionBy != null) {
					if (numberInputPorts != partitionBy.size())
						throw new IllegalArgumentException("The cardinality of parameter partitionBy (" + Integer.toString(partitionBy.size())
						                                    + ") must be equal the number of input ports (" +Integer.toString(numberInputPorts) + ") !");
					String[] attrs = partitionBy.get(i).split(",", -1);
					for (int j = 0; j < attrs.length; j++) {
						if ( ! attrs[j].isEmpty())
							portPartitonAttributeNames.add(attrs[j]);
					}
				}
			}
		
			//check every single entry in partitonAttributeNames for validity and add index to partitonAttributeIndexes
			//determine attributeIsPartitionKey, suppressIsPartitionKey, callbackIsPartitionKey
			for (int i = 0; i < numberInputPorts; i++) {
				StreamingInput<Tuple> port = context.getStreamingInputs().get(i);
				ArrayList<Integer> partIndx = partitonAttributeIndexes.get(i);
				if (port.getStreamWindow().isPartitioned()) {
					for (int j = 0; j < partitonAttributeNames.get(i).size(); j++) {
						String attributeName = partitonAttributeNames.get(i).get(j);
						int attributeIndex = port.getStreamSchema().getAttributeIndex(attributeName);
						if (attributeIndex < 0)
							throw new IllegalArgumentException("Input port " + Integer.toString(i) + " has no attribute with name " + attributeName);
						partIndx.add(new Integer(attributeIndex));
						if (attributeName.equals("attribute"))
							attributeIsPartitionKey.set(i, new Boolean(true));
						if (attributeName.equals("suppress"))
							suppressIsPartitionKey.set(i, new Boolean(true));
						if (attributeName.equals("callback"))
							callbackIsPartitionKey.set(i, new Boolean(true));
					}
				} else {
					ArrayList<String> portPartitonAttributeNames = partitonAttributeNames.get(i);
					if ((portPartitonAttributeNames.size() > 0) && ! portPartitonAttributeNames.get(0).isEmpty())
						throw new IllegalArgumentException("Input port " + Integer.toString(i) + " is not partitioned but has none empty partition name " + portPartitonAttributeNames.get(0));
				}
			}
		}
		System.out.println("partitonAttributeNames: " + partitonAttributeNames.toString());
		System.out.println("partitonAttributeIndexes: " + partitonAttributeIndexes.toString());
		System.out.println("attributeIsPartitionKey: " + attributeIsPartitionKey.toString());
		System.out.println("suppressIsPartitionKey: " + suppressIsPartitionKey.toString());
		System.out.println("callbackIsPartitionKey: " + callbackIsPartitionKey.toString());
		
		super.initialize(context);
	}

	//getter
	public boolean                       getNamedPartitionQuery()      { return namedPartitionQuery; }
	public boolean                       getAnyInputIsPartitioned()    { return anyInputIsPartitioned; }
	public ArrayList<ArrayList<String>>  getPartitonAttributeNames()   { return partitonAttributeNames; }
	public ArrayList<ArrayList<Integer>> getPartitonAttributeIndexes() { return partitonAttributeIndexes; }
	public ArrayList<Boolean>            getAttributeIsPartitionKey()  { return attributeIsPartitionKey; }
	public ArrayList<Boolean>            getSuppressIsPartitionKey()   { return suppressIsPartitionKey; }
	public ArrayList<Boolean>            getCallbackIsPartitionKey()   { return callbackIsPartitionKey; }

	static final String DESC = "REST HTTP or HTTPS API to view tuples from windowed input ports.\\n" + 
			"Embeds a Jetty web server to provide HTTP REST access to the collection of tuples in " + 
			"the input port window at the time of the last eviction for tumbling windows, " + 
			"or last trigger for sliding windows." +
			"\\n" +
			"The URLs defined by this operator are:\\n" +
			"* *prefix*`/ports/input/`*port index*`/tuples` - Returns the set of tuples as a array of the tuples in JSON format (content type `application/json`).\\n" +
			"* *prefix*`/ports/input/`*port index*`/info` - Output port meta-data including the stream attribute names and types (content type `application/json`).\\n" +
			"\\nThe *prefix* for the URLs is:\\n" +
			"* *context path*`/`*base operator name* - When the `context` parameter is set.\\n" +
			"* *full operator name* - When the `context` parameter is **not** set.\\n" +
			"\\n" + 
			"The `/tuples` URL accepts these optional query parameters:\\n" + 
			"* `partition` – When the window is partitioned defines the partition to be extracted from the window. When partitionKey contains multiple attributes, partition must contain the same number of values as attributes and in the same order, e.g. `?partition=John&partition=Smith`. " + 
			"would match the SPL partitionKey setting of: `partitionKey: “firstName”, “lastName”;`. When a window is partitioned and no partition query parameter is provided the data for all partitions is returned.\\n" + 
			"* `attribute` – Restricts the returned data to the named attributes. Data is returned in the order the attribute names are provided. When not provided, all attributes in the input tuples are returned. E.g. `?format=json&attribute=lastName&attribute=city` will return only the `lastName` and `city` attributes in that order with `lastName` first.\\n" + 
			"* `suppress` – Suppresses the named attributes from the output. When not provided, no attributes are suppressed. suppress is applied after applying the query parameter attribute. E.g. `?suppress=firstName&suppress=lastName` will not include lastName or firstName in the returned data.\\n" +
			"* `callback` – Wrappers the returned JSON in a call to callbackvalue(...json...); to enable JSONP processing.\\n" +
			"Query parameters are ignored if the input port's schema is `tuple<rstring jsonString>`.\\n" +
			"\\n" + 
			"The fixed URL `/ports/info` returns meta-data (using JSON) about all of the Streams ports that have associated URLs.\\n" + 
			"\\n" + 
			"Tuples are converted to JSON using " + 
			"the `JSONEncoding` support from the Streams Java Operator API,\\n" + 
			"except for: \\n" +
			"* If the input port's schema is `tuple<rstring jsonString>` then value is taken as is serialized JSON " +
			" and the resultant JSON is returned as the tuple's JSON value.\\n" +
			"* Within a tuple any attribute that is `rstring jsonString`, then the value is taken as " +
			"serialized JSON and it is placed into the tuple's " +
			"JSON object as its deserialized JSON with key `jsonString`.\\n" +
			"\\n" + 
			"`HTTPTupleView`, [HTTPTupleInjection], [HTTPXMLInjection] and [WebContext] embed a Jetty webserver and " + 
			"all operator invocations in an SPL application that are co-located/fused in same partition (PE) " + 
			"will share a common Jetty instance. Thus by " + 
			"fusing these operators together with a common port value, a single web-server serving a " + 
			"single SPL application can be created. This sharing of the web-server by fusing multiple " + 
			"operators allows the operators to be logically connected in the SPL graph, rather than a single " + 
			"operator with multiple unrelated streams being connected to it.\\n" + 
			"\\n" + 
			"Static content in the sub-directory `html` of the application's `opt` directory will also be served " + 
			"by the embedded web-server, thus allowing a complete web-application with live data to be " + 
			"served by an SPL application. The default URL for the web-server resolves to " + 
			"`{application_dir}/opt/html/index.html`.\\n" + 
			"\\n" + 
			"Operators that support the `context` and `contextResourceBase` SPL parameters will serve " + 
			"static files from the `contextResourceBase` directory rooted at the supplied context path.\\n" + 
			"\\n" + 
			"**Limitations**:\\n" + 
			"* Error handling is limited, incorrect URLs can crash the application, e.g. providing the wrong number of partition values.\\n" + 
			"* By default no security access is provided to the data, HTTPS must be explicitly configured.\\n"
			+ "# HTTPS Support and Sharing the Jetty Server\\n "
			+ "see also [namespace:com.ibm.streamsx.inet]";

}
