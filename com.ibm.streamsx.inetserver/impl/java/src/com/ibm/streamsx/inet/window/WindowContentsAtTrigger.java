/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2019, 2020  
*/
package com.ibm.streamsx.inet.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.streams.operator.Attribute;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streams.operator.window.StreamWindow;
import com.ibm.streams.operator.window.StreamWindowEvent;
import com.ibm.streams.operator.window.StreamWindowListener;
import com.ibm.streams.operator.window.StreamWindowPartitioner;
import com.ibm.streams.operator.window.WindowUtilities;
import com.ibm.streamsx.inet.rest.ops.TupleView;

/**
 * Window listener that provides a view of the window's
 * contents at the last trigger for sliding windows, or
 * eviction for tumbling windows.
 *
 * @param <T>
 */
public class WindowContentsAtTrigger<T> implements StreamWindowListener<T> {
	
	private final TupleView operator;
	private final int portIndex;
	private final OperatorContext context;
	private final StreamingInput<Tuple> input;

	private final boolean namedPartitionQuery;
	private final boolean forceEmpty;
	//lists must be empty if window is not partitioned
	private final ArrayList<String>  partitonAttributeNames;
	private final ArrayList<Integer> partitonAttributeIndexes;
	//the list of partition attributes in the order of definition; Size is 0 if window is not partitioned.
	private final List<Attribute> partitionAttributes;
	
	private final boolean isSliding;

	//the current window content
	private final Map<Object,List<T>> windowContents = Collections.synchronizedMap(new HashMap<Object,List<T>>());


	public WindowContentsAtTrigger(TupleView operator, int portIndex) {
		this.operator = operator;
		this.portIndex = portIndex;
		this.context = operator.getOperatorContext();
		this.input = context.getStreamingInputs().get(portIndex);

		namedPartitionQuery = operator.getNamedPartitionQuery();
		forceEmpty = operator.getForceEmpty();
		partitonAttributeNames = operator.getPartitonAttributeNames().get(portIndex);
		partitonAttributeIndexes = operator.getPartitonAttributeIndexes().get(portIndex);

		isSliding = StreamWindow.Type.SLIDING.equals(input.getStreamWindow().getType());
		
		if (!partitonAttributeNames.isEmpty()) {

			for (int i=0; i < partitonAttributeNames.size(); i++) {
				System.out.println("par: " + partitonAttributeNames.get(i));
			}
			if ( ! input.getStreamWindow().isPartitioned())
				throw new IllegalStateException("Window is not partitioned but has partitonAttributeNames");
			
			if (partitonAttributeNames.size() == 1) {
				WindowUtilities.registerAttributePartitioner(input.getStreamWindow(), partitonAttributeNames.toArray(new String[0]));
			} else {
				// RTC 14070
				// Multiple attributes.

				input.getStreamWindow().registerPartitioner(new StreamWindowPartitioner<Tuple,List<Object>>() {
					@Override
					public List<Object> getPartition(Tuple tuple) {
						final List<Object> attrs = new ArrayList<Object>(partitonAttributeIndexes.size());
						for (int i = 0; i < partitonAttributeIndexes.size(); i++)
							attrs.add(tuple.getObject(partitonAttributeIndexes.get(i)));
						return Collections.unmodifiableList(attrs);
					}
				});
			}

			List<Attribute> pa = new ArrayList<Attribute>();
			for (String attributeName : partitonAttributeNames)
				pa.add(input.getStreamSchema().getAttribute(attributeName));

			partitionAttributes = Collections.unmodifiableList(pa);

		} else {

			if (input.getStreamWindow().isPartitioned())
				throw new IllegalStateException("Window is partitioned but has no partitonAttributeNames");
			
			partitionAttributes = Collections.emptyList();

		}

	}

	@Override
	public synchronized void handleEvent(final StreamWindowEvent<T> event) throws Exception {
		final Object partition = event.getPartition();
		System.out.println("****** " + event.getType().toString());
		System.out.println("+++ " + partition.toString() + " +++");
		for (T tuple : event.getTuples()) {
			System.out.println(tuple.toString());
		}
		switch (event.getType()) {
		case EVICTION:
			if (isSliding)
				break;
			// fall through for a tumbling window
		case TRIGGER:
			List<T> tuples = new ArrayList<T>();
			for (T tuple : event.getTuples())
				tuples.add(tuple);
			if (tuples.isEmpty())
				windowContents.remove(partition);
			else
				windowContents.put(partition, tuples);
			break;
		case PARTITION_EVICTION:
			windowContents.remove(partition);
			break;
		default:
			break;

		}
	}
	
	public List<T> getWindowContents(Object partition) {
		if (partition == null)
			return getAllPartitions();

		if (partition instanceof Integer) {
			List<T> tuples = windowContents.get(partition);
			if (tuples == null)
				return Collections.emptyList();
			return Collections.unmodifiableList(tuples);
		}

		if ( ! namedPartitionQuery ) {
			
			List<T> tuples = windowContents.get(partition);
			if (tuples == null)
				return Collections.emptyList();
			return Collections.unmodifiableList(tuples);
			
		} else {
			
			//the case that only one partition key exists and was not entered as partition query is handled in if (partition == null)
			if (partitonAttributeNames.size() == 1) {
				List<T> tuples = windowContents.get(partition);
				if (tuples == null)
					return Collections.emptyList();
				return Collections.unmodifiableList(tuples);
			} else {
				@SuppressWarnings("unchecked")
				List<Object> myPart = (List<Object>)partition;
				Set<Object> allParititions = windowContents.keySet();
				List<T> allTuples = new ArrayList<T>();
				for (Object o : allParititions) {
					@SuppressWarnings("unchecked")
					List<Object> currentPart = (List<Object>)o;
					boolean match = true;
					for (int i = 0; i < currentPart.size(); i++) {
						if (myPart.get(i) != null) {
							if ( ! myPart.get(i).equals(currentPart.get(i))) {
								match = false;
								break;
							}
						}
					}
					if (match) {
						allTuples.addAll(windowContents.get(o));
					}
				}
				return allTuples;
			}
		}
	}
	
	private List<T> getAllPartitions() {
		List<T> allTuples = new ArrayList<T>();
		if (( ! forceEmpty ) || (partitonAttributeNames.size() == 0)) {
			synchronized (windowContents) {
				for (List<T> tuples : windowContents.values()) {
					allTuples.addAll(tuples);
				}
			}
		}
		return allTuples;
	}

	//Getter functions
	public OperatorContext getContext() { return context; }
	public StreamingInput<Tuple> getInput() { return input; }
	public List<Attribute> getPartitionAttributes() { return partitionAttributes; }
	public TupleView getOperator() { return operator; }
	public boolean getAttributeIsPartitionKey() { return operator.getAttributeIsPartitionKey().get(portIndex); }
	public boolean getSuppressIsPartitionKey() { return operator.getSuppressIsPartitionKey().get(portIndex); }
	public boolean getCallbackIsPartitionKey() { return operator.getCallbackIsPartitionKey().get(portIndex); }

}
