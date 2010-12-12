/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.historytree;

import com.google.protobuf.ByteString;

/** Interface for how aggregation is implemented. A is the type of an annotation, and 
 * V is the type of an event.*/

public interface AggregationInterface<A, V> {
	/** Factory for building aggregators, used to register aggregation schema's by name. */
	public interface Factory<A, V> {
		AggregationInterface<A, V> newInstance();
		String name();
	}	
	
    /** Get the name of this aggregation function. */
	String getName();
	/** Get any configuration information needed to configure it. (Eg, hash keys) */
	String getConfig();
	/** Set up the aggregator from the given config. */
	AggregationInterface<A,V> setup(String config);
	/** Aggregate the children. right may be null */
	A aggChildren(A leftAnn, A rightAnn);
	/** Map from an event to its aggregate. */
	A aggVal(V event);

	/** Representation of an empty stub agg. Cannot be null. (I think) */
	A emptyAgg();
	
	/** Serialize a value to a ByteString. */
	ByteString serializeVal(V val);
	/** Serialize an aggregate to a ByteString. */
	ByteString serializeAgg(A agg);
	/** Parse a ByteString to an aggregate. */
	A parseAgg(ByteString b);
	/** Parse a ByteString to a value. */
	V parseVal(ByteString b);
	/** Make a clone of this aggregation with the same fields. */
	AggregationInterface<A, V> clone();
}
