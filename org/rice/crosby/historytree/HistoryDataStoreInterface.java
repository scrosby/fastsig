package org.rice.crosby.historytree;

public interface HistoryDataStoreInterface<A,V> extends
		org.rice.crosby.historytree.NodeCursor.HistoryDataStore<A, V> {
	
	/* Operations that the history tree needs to be supported by the datastore */
	    NodeCursor<A,V> makeRoot(int layer); // Make a root at the given layer
	    void updateTime(int time);
}
