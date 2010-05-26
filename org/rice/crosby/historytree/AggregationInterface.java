package org.rice.crosby.historytree;

import com.google.protobuf.ByteString;

/** Interface for how aggregation is implemented. A is the type of an annotation, and 
 * V is the type of an event.*/

public interface AggregationInterface<A,V> {
	/** Factory for building aggregation, used to register aggregation schema's by name. */
	public interface Factory<A,V> {
		public AggregationInterface<A,V> newInstance();
		public String name();
	}	
	
    /** Get the name of this aggregation function */
	abstract String getName();
	/** Get any configuration information needed to configure it. (Eg, hash keys) */
	abstract String getConfig();
	/** Set up the aggregator from the given config */
	abstract AggregationInterface<A,V> setup(String config);
	/** Aggregate the children. right may be null */
	abstract A aggChildren(A leftAnn, A rightAnn);
	/** Map from an event to its aggregate */
	abstract A aggVal(V event);

	/** Representation of an empty stub agg. Cannot be null. (I think) */
	abstract A emptyAgg();
	
	/** Serialize a value to a ByteString */
	abstract ByteString serializeVal(V val);
	/** Serialize an aggregate to a ByteString */
	abstract ByteString serializeAgg(A agg);
	/** Parse a ByteString to an aggregate */
	abstract A parseAgg(ByteString b);
	/** Parse a ByteString to a value */
	abstract V parseVal(ByteString b);
	/** Make a clone of this aggregation with the same fields. */
	abstract AggregationInterface<A, V> clone();
}
