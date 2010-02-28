package org.rice.crosby.historytree;


import org.rice.crosby.historytree.generated.Serialization;



public class HistoryTree<A,V> {
	/** Operations that the history tree needs to be supported by the datastore */
	interface HistoryDataStore<A,V> {
	    NodeCursor<A,V> makeRoot(int layer); // Make a root at the given layer
	    void updateTime(int time);
	}

	/** Make an empty history tree with a given aggobj and datastore.  */
	public HistoryTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStore<A,V> datastore) {
	    this.time = -1;
		this.root = null;
		this.aggobj = aggobj;
		this.datastore = datastore;
	}

	/** Make an history at a given timestamp (used as a template for building a pruned trees)
	 */
	private HistoryTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStore<A,V> datastore,
	    		   int time) {
	    this.time = time;
		this.root = null;
		this.aggobj = aggobj;
		this.datastore = datastore;
	}

	//
	// Operations for adding to a log and getting the commitment
	//
	
	
	/** Add an event to the log */
	public void append(V val) {
		NodeCursor<A,V> leaf;
		if (time < 0) {
			time = 0;
			datastore.updateTime(time);
			root = leaf = datastore.makeRoot(0);
		} else {
			time = time+1;
			datastore.updateTime(time);
			reparent(time);
			leaf = forceLeaf(time);
		}
		leaf.setVal(val);
		computefrozenaggs(leaf);
	}
		
	private void reparent(int time) {
		while (!(time <= (1<<root.layer)-1))
			this.root = root.reparent();
	}

	/** Recurse from the leaf upwards, computing the agg for all frozen nodes */
    private void computefrozenaggs(NodeCursor<A,V> leaf) {
    	// First, set the leaf agg from the stored event (if it exists
    	if (leaf.hasVal() && leaf.getAgg() == null) 
    		leaf.setAgg(aggobj.aggVal(leaf.getVal()));

    	NodeCursor<A,V> node=leaf.getParent(root);
    	while (node != null && node.isFrozen(time)) {
    		node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
    		node = node.getParent(root);
    	}
    }
    

    public A agg() {
    	return aggV(time);
    }
    public A aggV(int version) {
    	NodeCursor<A,V>  child, leaf, node;

    	child = leaf = this.leaf(version);
    	node= leaf.getParent(root);
    	A agg = leaf.getAgg();

    	while (node!=null && version >= (1<<node.layer-1)) {
    		NodeCursor<A,V>  left = node.left();
    		if (child.equals(left))
    			agg = aggobj.aggChildren(agg,null);
    		else
    			agg = aggobj.aggChildren(left.getAgg(),agg);
    		child = node;
    		node = node.getParent(root);
    	}
    	return agg;
    }
    
    //
    //  Operations for making pruned trees.
    //
    
	/** Make a cursor pointing to the given leaf, if possible */
    private NodeCursor<A,V> leaf(int version)  {
    	if (time == 0)
    		return root;
    	NodeCursor<A,V> node=root,child;
    	for (int layer = log2(time) ; layer >= 0 ; layer--) {
    		int mask = 1 << (layer-1);
    		if ((mask & version) == mask)
    			child = node.right();
    		else
    			child = node.left();
    		if (layer == 1)
    			return child;
    		node = child;
    	}
    	assert false;
    	return null;
    }
    /** Make a cursor pointing to the given leaf, forcibly creating the path if possible */
    private NodeCursor<A,V> forceLeaf(int version) {
    	if (time == 0)
    		return root;
    	NodeCursor<A,V> node=root,child;
    	for (int layer = log2(time) ; layer >= 0 ; layer--) {
    		int mask = 1 << (layer-1);
    		if ((mask & version) == mask)
    			child = node.forceRight();
    		else
    			child = node.forceLeft();
    		if (layer == 1)
    			return child;
    		node = child;
    	}
    	assert false;
    	return null;
    }

    public HistoryTree<A,V> makePruned(HistoryDataStore<A, V> newdatastore) {
    	HistoryTree<A,V> out = new HistoryTree<A,V>(this.aggobj,newdatastore,this.time);
    	out.copyRoot(this);
    	return out;
        }
    
    private void copyRoot(HistoryTree<A,V> orig) {
    	assert this.root == null;
    	root = datastore.makeRoot(orig.root.layer);
    	if (root.isFrozen(time))
    		root.copyAgg(orig.root);
    }

    /** Make a path to one leaf and copy over its value or agg. 
     * @throws ProofError */
    private NodeCursor<A,V> copyVersionHelper(HistoryTree<A,V> orig, int version, boolean copyValFlag) throws ProofError {
    	NodeCursor<A,V> origleaf, selfleaf;
    	selfleaf = leaf(version);
    	origleaf = orig.leaf(version);
    	
    	if (selfleaf == null)
    		throw new ProofError("Leaf not in the tree");    	
    	selfleaf.copyAgg(origleaf);
    	// If we want a value, have one to copy from, and don't have one already... Copy it.
    	if (copyValFlag && !selfleaf.hasVal() && origleaf.hasVal())
    		selfleaf.copyVal(origleaf);
    	return origleaf;
    	}

    
    private void _copyAgg(HistoryTree<A,V> orig, NodeCursor<A,V> origleaf,NodeCursor<A,V> leaf) {
    	NodeCursor<A,V> node,orignode,origleft,origright;
    	node = leaf.getParent(root);
    	orignode = origleaf.getParent(orig.root);

    	// Invariant: We have a well-formed tree with all stubs include hashes EXCEPT possibly siblings in the path from the leaf to where it merged into the existing pruned tree.
   	    // Iterate up the tree, copying over sibling agg's for stubs. If we hit a node with two siblings. we're done. Earlier inserts will have already inserted sibling hashes for ancestor nodes.
    	while (orignode != null) {
    		if (node.left()!= null && node.right()!= null)
    			break;
    		origleft = orignode.left();
    		if (origleft.isFrozen(orig.time)) // TODO: Is this right? should it be this.time?
    			node.forceLeft().copyAgg(origleft);
    		
    		// A right node may or may not exist.
    		origright = orignode.right();
    		if (origright!= null && origright.isFrozen(orig.time)) // TODO: Is this right? should it be this.time?
    				node.forceRight().copyAgg(origright);
    			
    		orignode = orignode.getParent(orig.root);
    		node = node.getParent(root);
    	}
    }
    public void copyV(HistoryTree<A,V> orig, int version, boolean copyValueFlag) throws ProofError {
    	if (root == null)
    		copyRoot(orig);

    	NodeCursor<A,V> origleaf, selfleaf;
    	selfleaf = leaf(version);
    	origleaf = orig.leaf(version);

    	assert origleaf != null;
    	if (selfleaf!= null) {
    		// If the leaf is already in the tree...
    		assert selfleaf.getAgg() != null;
    		assert selfleaf.getAgg().equals(origleaf.getAgg());
    	} else {
    		selfleaf = copyVersionHelper(orig,version,copyValueFlag);
    		_copyAgg(orig,origleaf,selfleaf);    		
    	}    			
    	if (copyValueFlag) {
    		if (!origleaf.hasVal())
    			throw new ProofError("Missing value in proof");
    	} else {
    		if (!selfleaf.hasVal() && origleaf.hasVal()) 
    			selfleaf.copyVal(origleaf);
    	}
    
    }
    
    

    //
    //  Serialization code
    //
    void serializeNode(Serialization.HistNode.Builder out, NodeCursor<A,V> node) {
    	if (node.hasVal()) {
    		// Must be a leaf.
    		out.setVal(aggobj.serializeVal(node.getVal()));
    		return;
    	}
    	if (node.left() == null && node.right() == null) {
    		// Either a stub or a leaf. 
    		// Gotta include the agg for this node.
    		out.setAgg(aggobj.serializeAgg(node.getAgg()));
    		return;
    	}
    	// Ok, recurse both sides. Don't forget, we need to make a builder.
    	Serialization.HistNode.Builder b = Serialization.HistNode.newBuilder();
    	if (node.left() != null) {
    		serializeNode(b,node.left());
    		out.setLeft(b.build());
    		b.clear(); // Clear so we can reuse it.
    	}
    	if (node.right() != null) {
    		serializeNode(b,node.right());
    		out.setRight(b.build());
    	}
    }
    
    private void parseNode(NodeCursor<A,V> node, Serialization.HistNode in) {
    	if (in.hasVal()) {
    		V val = aggobj.parseVal(in.getVal());
    		node.setVal(val);
    		node.setAgg(aggobj.aggVal(val));
    		return;
    	}
    	if (in.hasAgg()) {
    		// If it has an agg, it should be a stub or a leaf stub.
    		assert !in.hasLeft();
    		assert !in.hasRight();
    		A agg = aggobj.parseAgg(in.getAgg());
    		node.setAgg(agg);
    		return;
    	}
    	// Must always have a left and right child.
    	assert in.hasLeft();
    	assert in.hasRight();
    	parseNode(node.forceLeft(),in.getLeft());
    	parseNode(node.forceRight(),in.getRight());
    }
    
    public String toString(String prefix) {
    	StringBuilder b = new StringBuilder();
    	debugString(b,prefix,root);
    	return new String(b);
    }
    public String toString() {
    	return toString("");
    }
	public void debugString(StringBuilder b, String prefix, NodeCursor<A,V> node) {
		b.append(prefix);
		b.append(":\t");
		// Print out the value (if any)
		if (node.hasVal())
			b.append(valToString(node.getVal()));
		else
			b.append("--");
		b.append("\t");	

		A agg = node.getAgg();
		if (agg==null)
			b.append(aggToString(agg));
		else
			b.append("<None>");

		b.append("\n");
		NodeCursor<A,V> left=node.left(), right=node.right();
		
		if (left != null)
			debugString(b,prefix+"L",left);
		if (right != null) 
			debugString(b,prefix+"R",right);
	}
    
    protected String aggToString(A a) {
    	return aggobj.serializeAgg(a).toString();
    }
    protected String valToString(V v) {
    	return aggobj.serializeVal(v).toString();
    }
 
    //
    //  Member fields
    //    
    private int time;
    private NodeCursor<A,V> root;
    private HistoryDataStore<A,V> datastore;
    private AggregationInterface<A,V> aggobj;

    // Misc helpers
    public static int log2(int x) {
    	int i = 0, pow = 1;
    	while (pow <= x) {
    		pow = pow*2;
    		i=i+1;
    	}
    	return i;
    }
}
