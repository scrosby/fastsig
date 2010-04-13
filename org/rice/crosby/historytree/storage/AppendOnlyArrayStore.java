package org.rice.crosby.historytree.storage;

import org.rice.crosby.historytree.NodeCursor;


public class AppendOnlyArrayStore<A,V> extends ArrayStoreBase<A,V>  {

	public AppendOnlyArrayStore() {
		super();
	}
	
	@Override
	public NodeCursor<A, V> makeRoot(int layer) {
		return new NodeCursor<A,V>(this,layer,0);
	}

	@Override
	public boolean isAggValid(NodeCursor<A, V> node) {
		return node.index <= time;
	}

	@Override
	public void markValid(NodeCursor<A, V> node) {
		assert(node.index <= time);
	}

	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;		

		
		while (time+1 > valstore.size())
			valstore.add(null);
		while (2*time+1 > aggstore.size())
			aggstore.add(null);
	}

}

