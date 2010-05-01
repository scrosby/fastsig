package org.rice.crosby.historytree;

/** Operations that the history tree needs to be supported by a datastore. 
 * 
 * This includes all operations that a cursor requires and some additional ones. 
 * */
public interface HistoryDataStoreInterface<A,V> extends
		org.rice.crosby.historytree.NodeCursor.HistoryDataStore<A, V> {
	
    /** Make a new cursor corresponding to a root node at the requested layer */
	    NodeCursor<A,V> makeRoot(int layer); // Make a root at the given layer

	/** Indicate to the data store what the current tree version is, used for designing the depth of the tree */
	    void updateTime(int time);
}
