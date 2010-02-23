package org.rice.crosby.historytree;

/** Interface for how aggregation is implemented. A is the type of an annotation, and 
 * V is the type of an event.*/

public interface AggregationInterface<A,V> {
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
}
