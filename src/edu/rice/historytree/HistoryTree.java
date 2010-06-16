package edu.rice.historytree;


import edu.rice.historytree.generated.Serialization;




public class HistoryTree<A,V> extends TreeBase<A,V> {
	/** Make an empty merkle tree with a given aggobj and datastore.  */
	public HistoryTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStoreInterface<A,V> datastore) {
	    super(aggobj,datastore);
	}
	@Override
	public A agg() {
    	return aggV(time);
    }

	/** Get the aggregate at a particular historical version number */
    public A aggV(int version) {
    	assert (version <= time);
    	NodeCursor<A,V>  child, leaf, node;

    	child = leaf = this.leaf(version);
    	node = leaf.getParent(root);
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

    
    void parseSubtree(NodeCursor<A,V> node, Serialization.HistNode in) {
    	if (parseNode(node,in))
    		return; // If its a stub.

    	// Not a stub. Must always have a left and may have right child.
    	assert in.hasLeft();

    	parseSubtree(node.forceLeft(), in.getLeft());

    	if (in.hasRight()) {
    		parseSubtree(node.forceRight(), in.getRight());
    		if (node.isFrozen(time)) {
    			node.markValid();
    			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
    		}
    	}
    }
}
