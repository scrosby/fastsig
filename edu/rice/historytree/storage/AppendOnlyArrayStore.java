package edu.rice.historytree.storage;

import edu.rice.historytree.NodeCursor;

/** An array store indended soley for append-only operation, with all nodes up to the last one are always valid.
 */
public class AppendOnlyArrayStore<A,V> extends ArrayStoreBase<A,V>  {

	public AppendOnlyArrayStore() {
		super();
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
	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;		

		
		while (time+1 > valstore.size())
			valstore.add(null);
		while (2*time+1 > aggstore.size())
			aggstore.add(null);
	}

}


