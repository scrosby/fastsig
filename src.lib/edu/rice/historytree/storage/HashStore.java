
package edu.rice.historytree.storage;

import java.util.HashMap;

import edu.rice.historytree.HistoryDataStoreInterface;
import edu.rice.historytree.NodeCursor;

public class HashStore<A,V> extends StoreBase implements HistoryDataStoreInterface<A, V> {
	private HashMap<Integer,A>  aggstore;
	private HashMap<Integer,V>  valstore;

	public HashStore() {
		this.aggstore = new HashMap<Integer,A>();
		this.valstore = new HashMap<Integer,V>();
	}
	@Override
	public NodeCursor<A, V> makeRoot(int layer) {
		return new NodeCursor<A,V>(this,layer,0);
	}

	@Override
	public A getAgg(NodeCursor<A, V> node) {
		Integer key=new Integer(node.computeIndex());
		//System.out.println("GetAgg "+key+"["+"]"+aggstore.get(key));
		return aggstore.get(key); 
		}

	@Override
	public V getVal(NodeCursor<A, V> node) {
		return valstore.get(new Integer(node.index()));
	}

	@Override
	public boolean hasVal(NodeCursor<A, V> node) {
		return valstore.get(new Integer(node.index())) != null;
	}

	@Override
	public boolean isAggValid(NodeCursor<A, V> node) {
		return aggstore.containsKey(new Integer(node.computeIndex()));
	}

	@Override
	public void markValid(NodeCursor<A, V> node) {
		Integer key=new Integer(node.computeIndex());
		if (!aggstore.containsKey(key))
			aggstore.put(key,null);
	}

	@Override
	public void setAgg(NodeCursor<A, V> node, A a) {
		assert(isAggValid(node));
		Integer key=new Integer(node.computeIndex());
		//System.out.println("SetAgg "+key+"["+node+"] = "+a);
		aggstore.put(key,a);
	}

	@Override
	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.put(new Integer(node.index()),v);
	}

	@Override
	public void updateTime(int time) {
		assert (time >= this.time);
		this.time = time;		
	}

	}
