package org.rice.crosby.historytree;

import org.rice.crosby.historytree.generated.Serialization;
import org.rice.crosby.historytree.generated.Serialization.HistNode;
import org.rice.crosby.historytree.storage.AppendOnlyArrayStore;

/** DOES NOT WORK WITH APPENDONLYARRAY */
public class MerkleTree<A, V> extends TreeBase<A, V> {	
	/** Make an empty merkle tree with a given aggobj and datastore.  */
	public MerkleTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStoreInterface<A,V> datastore) {
		super(aggobj,datastore);
		if (datastore instanceof AppendOnlyArrayStore<?,?>) 
			throw new Error("Merkle Tree incompatible with AppendOnlyArrayStore");
	}

	public MerkleTree<A, V> makePruned(HistoryDataStoreInterface<A, V> newdatastore) {
    	MerkleTree<A,V> out = new MerkleTree<A,V>(this.aggobj,newdatastore);
    	out.updateTime(this.time);
        out.root = out.datastore.makeRoot(root.layer);
    	out.isFrozen = true;
    	out.root.copyAgg(this.root);
    	return out;
	}
	
	
	public void freezeHelper(NodeCursor<A,V> node) {
  		node.markValid();
  		if (node.right() == null)
  			node.forceRight().setAgg(aggobj.emptyAgg()); // Force every right child to be valid, will keep the NULL default agg.
  		node.setAgg(aggobj.aggChildren(node.left().getAgg(), node.right().getAgg()));
  		}	  			
	
	/** Freeze the tree. After this, nothing else should be added to the Merkle tree. 
	 * Until being frozen, proofs and subtrees must not be built.
	 *	
	 * Take every node in path to leaf and mark it valid and set as a stub with a precomputed agg.
 	 */
	public void freeze() {
		assert (isFrozen == false);
		isFrozen = true;
	  	if (time <= 0)
	  		return;
	  	NodeCursor<A,V> node=leaf(time);
	  	while ((node=node.getParent(root)) != null) {
	  		freezeHelper(node);
	  	}
	}
	
	boolean isFrozen = false;
	
	@Override
	public A agg() {
		if (!isFrozen) 
			throw new Error("Cannot compute agg from unfrozen MerkleTree");
		if (root == null)
			return aggobj.emptyAgg();
		return root.getAgg();
	}

	@Override
	public void parseTree(Serialization.PrunedTree in) {
		super.parseTree(in);
		isFrozen = true;
	}
		
	@Override
	void parseSubtree(NodeCursor<A, V> node, HistNode in) {
    	if (parseNode(node,in))
    		return; // If its a stub.

    	parseSubtree(node.forceLeft(),in.getLeft());
    	parseSubtree(node.forceRight(),in.getRight());
    	node.markValid();
    	node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
	}
}
