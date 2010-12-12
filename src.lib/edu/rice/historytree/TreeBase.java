/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.historytree;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.rice.historytree.generated.Serialization;
import edu.rice.historytree.generated.Serialization.PrunedTree;

/**
 * Abstract class for storing a history tree, which is a complete tree,
 * resembling a heap. New levels are added on automatically.
 * 
 * @author crosby
 * 
 * @param <A>
 *            Type of an aggregate (Hash or string or X)
 * @param <V>
 *            Type of the value being stored.
 */
public abstract class TreeBase<A, V> {
	protected int time;
	protected NodeCursor<A, V> root;
	protected HistoryDataStoreInterface<A, V> datastore;
	protected AggregationInterface<A, V> aggobj;

	/** Make an empty history tree with a given aggobj and datastore. */
	public TreeBase(AggregationInterface<A, V> aggobj,
			HistoryDataStoreInterface<A, V> datastore) {
		this.time = -1;
		this.root = null;
		this.aggobj = aggobj;
		this.datastore = datastore;
	}

	/**
	 * Make an history at a given timestamp (used as a template for building a
	 * pruned trees or parsing trees.)
	 */
	public TreeBase<A, V> updateTime(int time) {
		this.time = time;
		datastore.updateTime(time);
		return this;
	}

	/**
	 * Adds the current tree as the left child of a new tree until the tree can
	 * hold a message of the requested timestamp.
	 * 
	 * @param time
	 */
	protected void reparent(int time) {
		while (!(time <= (1 << root.layer()) - 1))
			this.root = root.reparent();
	}

	/** Get a clone of the aggregation object */
	public AggregationInterface<A, V> getAggObj() {
		return aggobj.clone();
	}

	/**
	 * Get the version of the history tree. Tree contains $time+1$ events at
	 * indices [0...time]
	 */
	public int version() {
		return time;
	}

	/** Helper method for prettyprinting a tree */
	public String toString(String prefix) {
		StringBuilder b = new StringBuilder();
		b.append(prefix);
		b.append("  version = ");
		b.append(time);
		b.append("\n");
		debugString(b, prefix, root);
		return new String(b);
	}

	public String toString() {
		return toString("");
	}

	public void debugString(StringBuilder b, String prefix,
			NodeCursor<A, V> node) {
		b.append(prefix);
		b.append("\t" + toString(node));
		b.append("\n");

		if (node.isLeaf())
			return;

		NodeCursor<A, V> left = node.left(), right = node.right();

		if (left != null)
			debugString(b, prefix + "L", left);
		if (right != null)
			debugString(b, prefix + "R", right);
	}

	/** Return this as a longer tab-delimited string */
	public String toString(NodeCursor<A, V> node) {
		StringBuilder b = new StringBuilder();
		b.append(String.format("<%d,%d>", node.layer(), node.index()));
		b.append("\t");
		// Print out the value (if any)

		if (node.isLeaf() && node.hasVal())
			b.append(valToString(node.getVal()));
		else
			b.append("<>");

		b.append("\t");

		if (node.isAggValid()) {
			A agg = node.getAgg();
			if (agg == null)
				b.append("Null");
			else
				b.append(aggToString(agg));
		} else
			b.append("<>");
		// b.append(":");
		// b.append(datastore.hashCode());
		return b.toString();
	}

	/** Helper wrapper function for prettyprinting */
	private String aggToString(A a) {
		return aggobj.serializeAgg(a).toStringUtf8();
	}

	/** Helper wrapper function for prettyprinting */
	private String valToString(V v) {
		return aggobj.serializeVal(v).toStringUtf8();
	}

	/** Serialize a pruned tree to a protocol buffer */
	public void serializeTree(Serialization.PrunedTree.Builder out) {
		out.setVersion(time);
		if (root != null) {
			Serialization.HistNode.Builder builder = Serialization.HistNode
					.newBuilder();
			serializeNode(builder, root);
			out.setRoot(builder.build());
		}
	}

	/** Serialize a tree into a protobuf and serialize that into a byte array */
	public byte[] serializeTree() {
		Serialization.PrunedTree.Builder builder = Serialization.PrunedTree
				.newBuilder();
		serializeTree(builder);
		return builder.build().toByteArray();
	}

	/** Helper function for recursively serializing a history tree */
	private void serializeNode(Serialization.HistNode.Builder out,
			NodeCursor<A, V> node) {
		if (node.isLeaf()) {
			// System.out.println("SN:"+node);
			if (node.hasVal())
				out.setVal(aggobj.serializeVal(node.getVal()));
			else
				out.setAgg(aggobj.serializeAgg(node.getAgg()));
			return;
		}
		if (node.left() == null && node.right() == null) {
			// Either a stub or a leaf.
			// Gotta include the agg for this node.
			out.setAgg(aggobj.serializeAgg(node.getAgg()));
			return;
		}
		// Ok, recurse both sides. Don't forget, we need to make a builder.
		if (node.left() != null) {
			Serialization.HistNode.Builder b = Serialization.HistNode
					.newBuilder();
			serializeNode(b, node.left());
			out.setLeft(b.build());
		}
		if (node.right() != null) {
			Serialization.HistNode.Builder b = Serialization.HistNode
					.newBuilder();
			serializeNode(b, node.right());
			out.setRight(b.build());
		}
	}

	/**
	 * Make a cursor pointing to the given leaf, if possible.
	 * 
	 * @return The cursor for the relevant leaf node or null if the leaf is not
	 *         in the tree.
	 */
	public NodeCursor<A, V> leaf(int version) {
		if (version > time)
			throw new Error(String.format("Leaf %d in tree version %d",
					version, time));
		if (time == 0)
			return root;
		NodeCursor<A, V> node = root, child;
		for (int layer = log2(time);; layer--) {
			// System.out.println("leaf"+node);
			int mask = 1 << (layer - 1);
			if ((mask & version) == mask)
				child = node.right();
			else
				child = node.left();
			if (child == null)
				return null;
			if (layer == 1)
				return child;
			node = child;
		}
	}

	/**
	 * Make a cursor pointing to the given leaf, forcibly creating the path if
	 * possible
	 */
	protected NodeCursor<A, V> forceLeaf(int version) {
		if (time == 0)
			return root.markValid();
		NodeCursor<A, V> node = root, child;
		for (int layer = log2(time);; layer--) {
			// System.out.println("forceleaf"+node);
			int mask = 1 << (layer - 1);
			if ((mask & version) == mask)
				child = node.forceRight();
			else
				child = node.forceLeft();
			if (layer == 1)
				return child;
			node = child;
		}
	}

	/** Add an event to the history tree or merkle tree. */
	public void append(V val) {
		NodeCursor<A, V> leaf;
		if (time < 0) {
			time = 0;
			datastore.updateTime(time);
			root = leaf = datastore.makeRoot(0);
		} else {
			time = time + 1;
			datastore.updateTime(time);
			reparent(time);
			leaf = forceLeaf(time);
		}
		leaf.setVal(val);
		computefrozenaggs(leaf);
	}

	/**
	 * Recurse from the leaf upwards, computing the agg for all frozen nodes.
	 * 
	 * Frozen node == The subtree is complete and nothing more can be added.
	 */
	private void computefrozenaggs(NodeCursor<A, V> leaf) {
		// First, set the leaf agg from the stored event (if it exists
		if (leaf.hasVal() && leaf.getAgg() == null) {
			leaf.markValid();
			leaf.setAgg(aggobj.aggVal(leaf.getVal()));
		}
		NodeCursor<A, V> node = leaf.getParent(root);
		// System.out.println("Adding leaf "+leaf+" ------------------------ "
		// );
		while (node != null && node.isFrozen(time)) {
			assert (node.getAgg() == null);
			// System.out.println("Adding leaf "+leaf+" visit node" +node);
			node.setAgg(aggobj.aggChildren(node.left().getAgg(), node.right()
					.getAgg()));
			node = node.getParent(root);
		}
	}

	/**
	 * Copy the path to a given leaf from the original tree into this (pruned)
	 * tree.
	 * 
	 * @param leafnum
	 *            Which leaf to copy?
	 * @param copyValueFlag
	 *            Should the value under the given leaf be copied, or just the
	 *            annotation?
	 * 
	 * */
	public void copyV(TreeBase<A, V> orig, int leafnum, boolean copyValueFlag)
			throws ProofError {
		if (leafnum < 0 || leafnum > version())
			throw new IllegalArgumentException(String.format(
					"Version %d beyond the bounds of the tree [0,%d]", leafnum,
					version()));
		// If source tree is null
		if (root == null) {
			root = datastore.makeRoot(orig.root.layer());
		}

		NodeCursor<A, V> origleaf, selfleaf;
		selfleaf = forceLeaf(leafnum);
		origleaf = orig.leaf(leafnum);

		assert origleaf.getAgg() != null;

		if (!origleaf.isAggValid())
			throw new ProofError("Leaf not in the tree");

		if (selfleaf.isAggValid() && selfleaf.getAgg() != null) {
			// If the leaf is already in the tree...
			assert selfleaf.getAgg().equals(origleaf.getAgg());
		} else {
			selfleaf.copyAgg(origleaf);
		}
		// If we want a value
		if (copyValueFlag) {
			// have one to copy from
			if (!origleaf.hasVal()) {
				throw new ProofError("Leaf source does not have value to copy");
			}
			// and don't have one already
			if (!selfleaf.hasVal()) {
				selfleaf.copyVal(origleaf); // Copy it.
			}
		}

		copySiblingAggs(orig, origleaf, selfleaf, false);
	}

	/** Parse a given node into the requested cursor. */
	abstract void parseSubtree(NodeCursor<A, V> node, Serialization.HistNode in);

	/** Compute the aggregate value for a subtree. */
	abstract public A agg();

	/**
	 * Parse from a protocol buffer. I assume that 'this' history tree has been
	 * configured with the right aggobj and a datastore.
	 */
	public void parseTree(Serialization.PrunedTree in) {
		this.time = in.getVersion();
		if (in.hasRoot()) {
			root = datastore.makeRoot(log2(in.getVersion()));
			parseSubtree(root, in.getRoot());
		}
	}

	/** Parse a tree from a serialized protocol buffer */
	public void parseTree(byte data[]) throws InvalidProtocolBufferException {
		parseTree(PrunedTree.parseFrom(data));
	}

	/**
	 * Parse one node.
	 * 
	 * @return Returns true if this node is this node is a stub. I.e. if this
	 *         node has an agg or value attached. *
	 * @param node
	 *            A cursor pointing to the node to be changed.
	 * @param in
	 *            The corresponding protobuf object.
	 */
	protected boolean parseNode(NodeCursor<A, V> node, Serialization.HistNode in) {
		if (in.hasVal()) {
			V val = aggobj.parseVal(in.getVal());
			node.setVal(val);
			node.setAgg(aggobj.aggVal(val));
			return true;
		}
		if (in.hasAgg()) {
			// If it has an agg, it should be a stub or a leaf stub.
			assert !in.hasLeft();
			assert !in.hasRight();
			A agg = aggobj.parseAgg(in.getAgg());
			node.setAgg(agg);
			return true;
		}
		return false;
	}

	/**
	 * Traverse from a leaf to the root, copying any sibling agg objects, as
	 * appropriate.
	 * 
	 * To make a well-formed pruned tree, we need to include a path to whatever
	 * leaf. We also need to ensure that each sibling node on that path that is
	 * a stub also has an agg.
	 * 
	 * @param orig
	 *            The original tree.
	 * @param origleaf
	 *            The leaf in the original tree we're copying siblings from.
	 * @param leaf
	 *            The leaf in this tree we're copyingto.
	 * @param force
	 *            Do we copy siblings all the way to the root unconditionally?
	 *            Used when the pruned tree violates the invariant of all but
	 *            the newly added leaf containing sibling aggs.
	 * 
	 * 
	 * */
	protected void copySiblingAggs(TreeBase<A, V> orig,
			NodeCursor<A, V> origleaf, NodeCursor<A, V> leaf, boolean force) {
		assert (orig.time == this.time); // Except for concurrent
											// copies&updates, time shouldn't
											// change.
		NodeCursor<A, V> node, orignode;
		orignode = origleaf.getParent(orig.root);
		node = leaf.getParent(root);

		// Do we continue up the tree?
		boolean continuing = true;
		// Invariant: We have a well-formed tree with all stubs include hashes
		// EXCEPT possibly siblings in the path from the given leaf to where it
		// merged
		// into the existing pruned tree.

		// Iterate up the tree, copying over sibling agg's for stubs. If we hit
		// a node with two siblings. we're done. Earlier inserts will have
		// already inserted sibling hashes for ancestor nodes.
		while (continuing && node != null) {
			if (!force && node.left() != null && node.right() != null)
				continuing = false;
			NodeCursor<A, V> origleft, origright;
			origleft = orignode.left();
			if (origleft != null && origleft.getAgg() != null)
				node.forceLeft().copyAgg(origleft);

			// A right node may or may not exist.
			origright = orignode.right();
			if (origright != null && origright.getAgg() != null)
				node.forceRight().copyAgg(origright);

			orignode = orignode.getParent(orig.root);
			node = node.getParent(root);
		}
		// Handle the root-is-frozen case
		if (root.isFrozen(time)) {
			root.markValid();
			root.copyAgg(orig.root);
		}
	}

	/** Return ceil(log_2(x)) */
	public static int log2(int x) {
		int i = 0, pow = 1;
		while (pow <= x) {
			pow = pow * 2;
			i = i + 1;
		}
		return i;
	}

	/**
	 * Make an empty pruned tree based around the current tree.
	 * 
	 * @param datastore
	 *            The datastore to used for the pruned tree.
	 * @return
	 */
	abstract public TreeBase<A, V> makePruned(
			HistoryDataStoreInterface<A, V> datastore);

}
