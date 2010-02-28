package org.rice.crosby.historytree;

import java.util.Vector;

import org.rice.crosby.historytree.HistoryTreeOps.NodeFactoryInterface;

public class ArrayStore<A,V> extends StoreBase implements NodeFactoryInterface<A, V>,
		org.rice.crosby.historytree.NodeCursor.NodeFactoryInterface<A, V> {

	public ArrayStore() {
		this.time = -1;
		this.aggstore = new Vector<A>();
		this.valstore = new Vector<V>();
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
		aggstore.set(node.computeIndex(),a);
	}

	@Override
	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.set(node.computeIndex(),v);
	}

	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;		
		aggstore.add(time,null);
		valstore.add(time,null);
	}

	protected int time;
	private Vector<A>  aggstore;
	private Vector<V>  valstore;
	
}


