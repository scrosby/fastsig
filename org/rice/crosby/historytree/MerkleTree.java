package org.rice.crosby.historytree;

import org.rice.crosby.historytree.generated.Serialization.HistNode;

/** DOES NOT WORK WITH APPENDONLYARRAY */
public class MerkleTree<A, V> extends TreeBase<A, V> {

	/** Freeze the tree. After this, nothing else should be added to the Merkle tree. 
	 * Until being frozen, proofs and subtrees must not be built.
	 */
	public void freeze() {
	  	if (time == 0)
	  		return;
	  	NodeCursor<A,V> node=root,child;
	  	for (int layer = log2(time) ; layer >= 0 ; layer--) {
	  		int mask = 1 << (layer-1);
	  			
	  		if (!node.isFrozen(time))
	  			node.forceRight(); // Force every right child to be valid, will keep the NULL default agg.
	  		
	  		if ((mask & time) == mask)
	  			child = node.right();
	  		else
	  			child = node.left();
	  		if (layer == 1)
	  			return;
	  		node = child;
	  	}
	  	assert false;
	}
	
	@Override
	public A agg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void copySiblingAggs(TreeBase<A, V> orig, NodeCursor<A, V> origleaf,
			NodeCursor<A, V> leaf, boolean force) {
		// TODO Auto-generated method stub
		
	}

	@Override
	void parseNode(NodeCursor<A, V> node, HistNode in) {
		// TODO Auto-generated method stub
		
	}

}
