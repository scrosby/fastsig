package org.rice.crosby.historytree;


import org.rice.crosby.historytree.generated.Serialization;




public class HistoryTree<A,V> extends TreeBase<A,V> {


	/** Make an empty history tree with a given aggobj and datastore.  */
	public HistoryTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStoreInterface<A,V> datastore) {
	    this.time = -1;
		this.root = null;
		this.aggobj = aggobj;
		this.datastore = datastore;
	}

	public A agg() {
    	return aggV(time);
    }
    public A aggV(int version) {
    	assert (version <= time);
    	NodeCursor<A,V>  child, leaf, node;

    	child = leaf = this.leaf(version);
    	node= leaf.getParent(root);
    	A agg = leaf.getAgg();
		//System.out.println("leaf"+node);

    	while (node!=null && version >= (1<<node.layer-1)) {
    		//System.out.println("aggv"+node);
    		NodeCursor<A,V>  left = node.left();
    		if (child.equals(left))
    			agg = aggobj.aggChildren(agg,null);
    		else {
    			A leftagg = left.getAgg(); assert leftagg != null;
    			agg = aggobj.aggChildren(leftagg,agg);
    		}
    		child = node;
    		node = node.getParent(root);
    	}
    	return agg;
    }
    
    //
    //  Operations for making pruned trees.
    //
    
	public HistoryTree<A, V> makePruned(HistoryDataStoreInterface<A, V> newdatastore) {
    	HistoryTree<A,V> out = new HistoryTree<A,V>(this.aggobj,newdatastore);
    	out.updateTime(this.time);
        out.root = out.datastore.makeRoot(root.layer);
    	out.copySiblingAggs(this,this.leaf(time),out.forceLeaf(time),true);
    	return out;
        }

    
    void copySiblingAggs(TreeBase<A, V> orig, NodeCursor<A,V> origleaf,NodeCursor<A,V> leaf, boolean force) {
		assert(orig.time == this.time); // Except for concurrent copies&updates, time shouldn't change.
    	NodeCursor<A,V> node,orignode;
    	orignode = origleaf.getParent(orig.root);
    	node = leaf.getParent(root);

    	boolean continuing = true;
    	// Invariant: We have a well-formed tree with all stubs include hashes EXCEPT possibly siblings in the path from the leaf to where it merged into the existing pruned tree.
   	    // Iterate up the tree, copying over sibling agg's for stubs. If we hit a node with two siblings. we're done. Earlier inserts will have already inserted sibling hashes for ancestor nodes.
    	while (continuing && node != null) {
    		//System.out.println("CA("+orig.version()+"): "+orignode+" --> "+node);
    		// FIX: THE INITAL TREE VIOLATES THE INVARIANTS.
    		if (!force && node.left() != null && node.right() != null)
    			continuing = false;
    		NodeCursor<A,V> origleft,origright;
    		//System.out.println("NO BREAK");
    		origleft = orignode.left();
    		//System.out.println("CL: "+origleft+" --> "+node.forceLeft());
    		if (origleft.isFrozen(this.time))
    			node.forceLeft().copyAgg(origleft);
    		
    		// A right node may or may not exist.
    		origright = orignode.right();
    		//System.out.println("RIGHT:"+origright+"  "+time); 
    		if (origright!= null && origright.isFrozen(time))
    				node.forceRight().copyAgg(origright);

    		//System.out.println("LOOP");
 		
    		orignode = orignode.getParent(orig.root);
    		node = node.getParent(root);
    	}
    	// Handle the root-is-frozen case
    	if (root.isFrozen(time)) {
    		root.markValid();
    		root.copyAgg(orig.root);
    	}
    }    
    void parseNode(NodeCursor<A,V> node, Serialization.HistNode in) {
    	if (parseThisNode(node,in))
    		return; // If its a stub.

    	// Not a stub. Must always have a left and may have right child.
    	assert in.hasLeft();

    	parseNode(node.forceLeft(),in.getLeft());

    	if (in.hasRight()) {
    		parseNode(node.forceRight(),in.getRight());
    		if (node.isFrozen(time)) {
    			node.markValid();
    			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
    		}
    	}
    }
}
