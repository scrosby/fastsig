package org.rice.crosby.historytree;

import java.util.ArrayList;
import java.util.Vector;

import org.rice.crosby.historytree.HistoryTree.HistoryDataStore;

public class ArrayStore<A,V> extends StoreBase implements HistoryDataStore<A, V>,
		org.rice.crosby.historytree.NodeCursor.HistoryDataStore<A, V> {

	public ArrayStore() {
		this.time = -1;
		this.aggstore = new ArrayList<A>();
		this.valstore = new ArrayList<V>();
	}
	
	@Override
	public NodeCursor<A, V> makeRoot(int layer) {
		return new NodeCursor<A,V>(this,layer,0);
	}

	@Override
	public A getAgg(NodeCursor<A, V> node) {
		int index = node.computeIndex();
		assert(index >= 0);
		if (index < aggstore.size())
			return aggstore.get(index);
		return null;
	}

	@Override
	public V getVal(NodeCursor<A, V> node) {
		return valstore.get(node.index);
	}

	@Override
	public boolean hasVal(NodeCursor<A, V> node) {
		return valstore.get(node.index) != null;
	}

	@Override
	public boolean isAggValid(NodeCursor<A, V> node) {
		return node.index <= time;
	}

	@Override
	public void markValid(NodeCursor<A, V> node) {
		assert(node.index <= time);
	}

	@Override
	public void setAgg(NodeCursor<A, V> node, A a) {
		aggstore.set(node.computeIndex(),a);
	}

	@Override
	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.set(node.index,v);
	}

	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;		

		if (time >= valstore.size())
			valstore.ensureCapacity(time+1);
		if (2*time+0 >= aggstore.size())
			aggstore.ensureCapacity(2*time+0+1);
	}

	protected int time;
	private ArrayList<A>  aggstore;
	private ArrayList<V>  valstore;
	
}


