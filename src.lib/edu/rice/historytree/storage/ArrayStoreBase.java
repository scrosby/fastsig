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

package edu.rice.historytree.storage;

import java.util.ArrayList;

import edu.rice.historytree.HistoryDataStoreInterface;
import edu.rice.historytree.NodeCursor;

/** Common base class for when a history tree is stored as two arrays.
 * 
 * Nodes in a tree are assigned offsets in the array based on their position in a post-order traversal of the history tree.
 * 
 * @author crosby
 *
 * @param <A>
 * @param <V>
 */
public abstract class ArrayStoreBase<A, V> extends StoreBase implements HistoryDataStoreInterface<A, V> {
    /** Record the agg for a node. Offset into the array is the node's index in a post order traversal. */
	protected ArrayList<A> aggstore;
    /** Record the val for a node. Offset into the array is the leaf node's index. */
	protected ArrayList<V> valstore;


	@Override
	public NodeCursor<A, V> makeRoot(int layer) {
		return new NodeCursor<A,V>(this,layer,0);
	}
	

	public A getAgg(NodeCursor<A, V> node) {
		int index = node.computeIndex();
		assert(index >= 0);
		if (index < aggstore.size())
			return aggstore.get(index);
		return null;
	}

	public V getVal(NodeCursor<A, V> node) {
		return valstore.get(node.index());
	}

	public boolean hasVal(NodeCursor<A, V> node) {
		return valstore.get(node.index()) != null;
	}

	public void setAgg(NodeCursor<A, V> node, A a) {
		assert(isAggValid(node));
		aggstore.set(node.computeIndex(),a);
	}

	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.set(node.index(),v);
	}
	

	public ArrayStoreBase() {
		super();
		this.aggstore = new ArrayList<A>(5);
		this.valstore = new ArrayList<V>(5);
	}

	abstract public boolean isAggValid(NodeCursor<A, V> node);

	abstract public void markValid(NodeCursor<A, V> node);	
	
	abstract public void updateTime(int time);
}