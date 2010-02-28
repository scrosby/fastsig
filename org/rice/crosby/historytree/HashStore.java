package org.rice.crosby.historytree;

import java.util.Hashtable;
import org.rice.crosby.historytree.HistoryTree.HistoryDataStore;

public class HashStore<A,V> extends StoreBase implements HistoryDataStore<A, V>,
		org.rice.crosby.historytree.NodeCursor.HistoryDataStore<A, V> {

	HashStore() {
		this.time = -1;
		this.aggstore = new Hashtable<Integer,A>();
		this.valstore = new Hashtable<Integer,V>();
	}
	@Override
	public NodeCursor<A, V> makeRoot(int layer) {
		return new NodeCursor<A,V>(this,layer,0);
	}

	@Override
	public A getAgg(NodeCursor<A, V> node) {
		return aggstore.get(node.computeIndex()); 
	}

	@Override
	public V getVal(NodeCursor<A, V> node) {
		return valstore.get(node.computeIndex());
	}

	@Override
	public boolean hasVal(NodeCursor<A, V> node) {
		return valstore.get(node.computeIndex()) != null;
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
		aggstore.put(new Integer(node.computeIndex()),a);
	}

	@Override
	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.put(new Integer(node.computeIndex()),v);
	}

	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;		
	}

	Hashtable<Integer,A>  aggstore;
	protected int time;
	Hashtable<Integer,V>  valstore;
}


