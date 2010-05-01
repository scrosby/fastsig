package org.rice.crosby.historytree.storage;

import java.util.ArrayList;

import org.rice.crosby.historytree.HistoryDataStoreInterface;
import org.rice.crosby.historytree.NodeCursor;

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
	protected int time;

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
		return valstore.get(node.index);
	}

	public boolean hasVal(NodeCursor<A, V> node) {
		return valstore.get(node.index) != null;
	}

	public void setAgg(NodeCursor<A, V> node, A a) {
		assert(isAggValid(node));
		aggstore.set(node.computeIndex(),a);
	}

	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.set(node.index,v);
	}

    /** Record the agg for a node. Offset into the array is the node's index in a post order traversal. */
	protected ArrayList<A> aggstore;
    /** Record the val for a node. Offset into the array is the leaf node's index. */
	protected ArrayList<V> valstore;

	public ArrayStoreBase() {
		super();
		this.time = -1;
		this.aggstore = new ArrayList<A>(5);
		this.valstore = new ArrayList<V>(5);
	}

	abstract public boolean isAggValid(NodeCursor<A, V> node);

	abstract public void markValid(NodeCursor<A, V> node);
	
	
	
}