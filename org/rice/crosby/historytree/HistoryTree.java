package org.rice.crosby.historytree;


import org.rice.crosby.historytree.generated.Serialization;
import org.rice.crosby.historytree.generated.Serialization.HistTree;

import com.google.protobuf.InvalidProtocolBufferException;



public class HistoryTree<A,V> extends TreeBase<A,V> {


	/** Make an empty history tree with a given aggobj and datastore.  */
	public HistoryTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStoreInterface<A,V> datastore) {
	    this.time = -1;
		this.root = null;
		this.aggobj = aggobj;
		this.datastore = datastore;
	}

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
		
	/** Compute any frozen aggregates on a node */
	private void computeAggOnNode(NodeCursor<A,V> node) {
		if (node.isLeaf()) {
			if (node.hasVal() && node.getAgg() == null) 
				node.setAgg(aggobj.aggVal(node.getVal()));
		} else {
			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
		}
	}
	
	/** Recurse from the leaf upwards, computing the agg for all frozen nodes */
    private void computefrozenaggs(NodeCursor<A,V> leaf) {
    	// First, set the leaf agg from the stored event (if it exists
    	if (leaf.hasVal() && leaf.getAgg() == null) {
    		leaf.markValid();
    		leaf.setAgg(aggobj.aggVal(leaf.getVal()));
    	}
    	NodeCursor<A,V> node=leaf.getParent(root);
    	//System.out.println("Adding leaf "+leaf+" ------------------------ " );
    	while (node != null && node.isFrozen(time) && node.getAgg() == null) {
        	//System.out.println("Adding leaf "+leaf+" visit node" +node);
    		node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
    		node = node.getParent(root);
    	}
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
    	out._copyAgg(this,this.leaf(time),out.forceLeaf(time),true);
    	return out;
        }

    /** Make a path to one leaf and copy over its value or agg. 
     * @throws ProofError */
    private NodeCursor<A,V> copyVersionHelper(HistoryTree<A,V> orig, int version, boolean copyValFlag) throws ProofError {
    	NodeCursor<A,V> origleaf, selfleaf;
    	selfleaf = forceLeaf(version);
    	origleaf = orig.leaf(version);
    	
    	if (!origleaf.isAggValid())
    		throw new ProofError("Leaf not in the tree");    	
    	selfleaf.copyAgg(origleaf);
    	// If we want a value, have one to copy from, and don't have one already... Copy it.
    	if (copyValFlag && !selfleaf.hasVal() && origleaf.hasVal())
    		selfleaf.copyVal(origleaf);
    	return selfleaf;
    	}

    
    private void _copyAgg(TreeBase<A, V> orig, NodeCursor<A,V> origleaf,NodeCursor<A,V> leaf, boolean force) {
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
    public void copyV(HistoryTree<A,V> orig, int version, boolean copyValueFlag) throws ProofError {
    	if (root == null) {
    		root = datastore.makeRoot(orig.root.layer);
    		//copyVersionHelper(orig,this.time,false);
    	}
    		
    	NodeCursor<A,V> origleaf, selfleaf;
    	selfleaf = forceLeaf(version);
    	origleaf = orig.leaf(version);

    	selfleaf.equals(root);
    	origleaf.equals(orig);
    	
   		//System.out.println("copyV"+selfleaf+"  "+origleaf);
   	    	
    	assert origleaf.getAgg() != null;
    	if (selfleaf.isAggValid() && selfleaf.getAgg() != null) {
    		// If the leaf is already in the tree...
    		assert selfleaf.getAgg().equals(origleaf.getAgg());
    	} else {
    		copyVersionHelper(orig,version,copyValueFlag);
    		_copyAgg(orig,origleaf,selfleaf,false);
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
    //  TODO: Serialization code
    //
    /*static HistoryTree makeFromConfig(Serialization.Config config) {
    	return null;
    }*/
    
    /** Parse from a protocol buffer. I assume that the history tree has 
     * been configured with the right aggobj and a datastore. */
    public void parseTree(Serialization.HistTree in) {
    	this.time = in.getVersion();
    	if (in.hasRoot()) {
    		root = datastore.makeRoot(log2(in.getVersion()));
    		parseNode(root,in.getRoot());    		
    	}
    }

    public void parseTree(byte data[]) throws InvalidProtocolBufferException {
		parseTree(HistTree.parseFrom(data));
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
    	parseNode(node.forceLeft(),in.getLeft());

    	if (in.hasRight()) {
    		parseNode(node.forceRight(),in.getRight());
    		if (node.isFrozen(time)) {
    			node.markValid();
    			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
    		}
    	}
    }
    
    public void mergeTree(TreeBase<A, V> peer) {
    	NodeCursor <A,V> thisroot, peerroot;

    	if (peer.version() <0) {
    		// Nothing to merge.
    		return;
    	}

    	if (this.version() <0 ) {
    		// Need to make a root node to act as the destination
    		this.root = new NodeCursor<A,V>(this.datastore,log2(peer.version()),0);
    		assert log2(peer.version()) == peer.root.layer;
    	}
    	
    	
    	thisroot = this.root;
    	peerroot = peer.root;

    	// Get two new roots on the same layer before merging.
    	if (this.version() < peer.version()) {
    		while (peerroot.layer > this.root.layer)
    			this.root = this.root.reparent();
    		this.updateTime(peer.version());
    		thisroot = this.root;
    	} else {
    		while (thisroot.layer > peerroot.layer)
    			thisroot=thisroot.left();
    	}

    	mergeNode(peer,thisroot,peerroot);
     }


    private void mergeNode(TreeBase<A, V> peer, NodeCursor<A,V> thisnode, NodeCursor<A,V> peernode) {
    	/*
    	 * 
    	 *  Invariant: The thisnode and peernode are always 'valid'
    	 */
    	assert thisnode.isAggValid();
    	assert peernode.isAggValid();

    	if (peernode.isLeaf()) {
    		if (peernode.hasVal()) 
    			thisnode.copyVal(peernode);
    		return;
    	}
    	if (peernode.isStub()) {
    		if (thisnode.getAgg() == null) {
    			peernode.copyAgg(thisnode);
    		}
    		return;
    	}

    	// OK, now recurse the left subtrees.
    	mergeNode(peer,thisnode.forceLeft(),peernode.left());

    	// Do we recurse the right trees? The peer is not a stub.
    	if (thisnode.right().index <= version() && peernode.right().index <= peer.version()) {
    		// Both have valid right children for this snapshot version.
    		mergeNode(peer,thisnode.forceRight(),peernode.right());
    		if (thisnode.isFrozen(time) && thisnode.getAgg() == null)
    			computeAggOnNode(thisnode);
    	}
    }
}
