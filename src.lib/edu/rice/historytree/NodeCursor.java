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

/**
 * A cursor for navigating around a a history tree.
 * 
 * The domain of a cursor is: { (layer,index) } UNION NULL
 * 
 * I use cursors to traverse trees stored in external data stores. At each
 * position, an external datastore can either be 'valid' and have data at that
 * location, be 'invalid' and not have data at that location.
 * 
 * I have a 3-way challenge in designing them and the underlying data store.
 * 
 * A given Cursor can be either:
 * 
 * None: Used to indicate NULL, eg, no such child. Indicated by NodeFactory ==
 * NULL
 * 
 * Valid: Pointing to a layer&index containing real data.
 * 
 * Invalid: Points to a valid location, however that location, if dereferenced,
 * has no valid data, so this node cannot be dereferenced. This is used to
 * denote an omitted node.
 * 
 * 
 * At a given slot in the datastore, the aggregate may be:
 * 
 * The domain of things stored in the datastore is None UNION set of all A. 
 * 
 * We need 'None' to place into a valid, but non-frozen tree node.
 * 
 * A blob of data.
 * 
 * A leaf should always have a value, unless it has been deliberately stubbed
 * out.
 */

public final class NodeCursor<A, V> {
	private final HistoryDataStore<A, V> datastore;
	private final int layer;
	private final int index;

	final public int layer() { return layer; }
	final public int index() { return index; }
	/** Interfaces that a cursor requires of a data store */
	interface HistoryDataStore<A, V> {
		/**
		 * A node cursor can point anywhere. This indicates that we should
		 * create the cursor location and allow aggregates to be stored there.
		 */
		void markValid(NodeCursor<A, V> node);

		/**
		 * A node cursor can point anywhere. This checks if the pointed-to location is valid.
		 */
		boolean isAggValid(NodeCursor<A, V> node);

		/**
		 * Set the aggregate for a particular cursor to a particular aggregate
		 * value. Must be non-null, and will only be applied to locations marked valid.
		 */
		void setAgg(NodeCursor<A, V> node, A a);

		/**
		 * Set the value for a particular cursor to a particular value. Must be
		 * non-null.
		 */
		void setVal(NodeCursor<A, V> node, V v);

		/**
		 * Get the aggregate for a particular cursor. Cursor location must be
		 * previously marked valid.
		 */
		A getAgg(NodeCursor<A, V> node);

		/** Get the value at the particular cursor, which should be a leaf. */
		V getVal(NodeCursor<A, V> node);

		/**
		 * Does the given leaf node have a value set? A leaf may have only an
		 * aggregate.
		 */
		boolean hasVal(NodeCursor<A, V> node);
	};

	public NodeCursor(HistoryDataStore<A, V> nodefactory, int layer, int index) {
		assert nodefactory != null;
		this.datastore = nodefactory;
		this.layer = layer;
		this.index = index;
	}

	/*
	 * Helper functions
	 */

	boolean isFrozen(int time) {
		return time >= index + getStep() - 1;
	}

	int getStep() {
		return 1 << layer;
	}

	boolean isLeaf() {
		return layer == 0;
	}

	/** Compute index in a total order */
	public int computeIndex() {
		assert index >= 0;
		assert layer >= 0;
		int s = 0;
		int j = index + (1 << layer) - 1;
		while (j > 0) {
			s = s + j;
			j = j / 2;
		}
		// System.out.format("\n %d %d --> %d \n",index,layer,s+layer);
		return s + layer;
	}

	/**
	 * Return a NodeCursor reference with the layer and index numbers of the
	 * given child. May or may not actually exist.
	 */
	NodeCursor<A, V> getLeft() {
		assert layer > 0;
		int newindex = index;
		return new NodeCursor<A, V>(datastore, layer - 1, newindex);
	}

	/**
	 * Return a NodeCursor reference with the layer and index numbers of the
	 * given child. May or may not actually exist.
	 */
	NodeCursor<A, V> getRight() {
		assert layer > 0;
		int newindex = index + getStep() / 2;
		return new NodeCursor<A, V>(datastore, layer - 1, newindex);
	}

	/** Make a cursor that is a parent of the current cursor and mark it valid. */
	NodeCursor<A, V> reparent() {
		return new NodeCursor<A, V>(datastore, layer + 1, 0).markValid();
	}

	/**
	 * Get the parent node of the current cursor. In order to return 'null' for
	 * the root node, to indicate that it is not the parent, we must know what
	 * the root is.
	 */
	NodeCursor<A, V> getParent(final NodeCursor<A, V> root) {
		// System.out.println("GetParent "+root+"==?"+this);
		if (this.equals(root))
			return null;
		// System.out.println("NonNull Parent");
		return new NodeCursor<A, V>(datastore, layer + 1, index
				& ~(getStep() * 2 - 1));
	}

	/**
	 * Get the node that is the left child of the current cursor, or null if the
	 * left child is not valid. (Meaning that the store does not have data at
	 * that position)
	 */
	NodeCursor<A, V> left() {
		NodeCursor<A, V> out = getLeft();
		if (out.isAggValid())
			return out;
		else
			return null;
	}

	/**
	 * Get the node that is the right child of the current cursor, or null if
	 * the left child is not valid. (Meaning that the store does not have data
	 * at that position)
	 */
	NodeCursor<A, V> right() {
		NodeCursor<A, V> out = getRight();
		if (out.isAggValid())
			return out;
		else
			return null;
	};

	/** A node is a leaf, stub, or interior */
	boolean isStub() {
		assert !isLeaf();
		return getLeft().isAggValid();
	}

	/**
	 * Return the layer and index numbers of the given child. Indicate to the
	 * store that it is valid and should be created if it does not already
	 * exist.
	 */
	NodeCursor<A, V> forceLeft() {
		return getLeft().markValid();
	}

	/**
	 * Return the layer and index numbers of the given child. Indicate to the
	 * store that it is valid and should be created if it does not already
	 * exist.
	 */
	NodeCursor<A, V> forceRight() {
		return getRight().markValid();
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (o instanceof NodeCursor<?, ?>) {
			NodeCursor<A, V> o2 = (NodeCursor<A, V>) o;
			assert (this.datastore == o2.datastore);
			if (this.layer != o2.layer)
				return false;
			if (this.index != o2.index)
				return false;
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(String.format("<%d,%d>", layer, index));
		b.append(",V=");
		// Print out the value (if any)

		if (isLeaf() && hasVal())
			b.append(getVal().toString());
		else
			b.append("<>");

		b.append(",A=");

		if (isAggValid()) {
			A agg = getAgg();
			if (agg == null)
				b.append("Null");
			else
				b.append(agg.toString());
		} else
			b.append("<>");
		// b.append(":");
		// b.append(datastore.hashCode());
		return b.toString();
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#isAggValid */
	NodeCursor<A, V> markValid() {
		datastore.markValid(this);
		return this;
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#isAggValid */
	boolean isAggValid() {
		return datastore.isAggValid(this);
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#hasVal */
	boolean hasVal() {
		return datastore.hasVal(this);
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#getVal */
	public V getVal() {
		V out = datastore.getVal(this);
		assert out != null;
		return out;
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#setVal */
	void setVal(V v) {
		assert (v != null);
		datastore.setVal(this, v);
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#getAgg */
	public A getAgg() {
		return datastore.getAgg(this);
	}

	/** Reflects onto the data store. 
	 * 
	 * @see HistoryDataStore#setAgg */
	void setAgg(A v) {
		assert (v != null);
		datastore.setAgg(this, v);
	}

	/** Copy the aggregate from the node pointed to by the cursor */
	void copyAgg(NodeCursor<A, V> orig) {
		assert orig.getAgg() != null;
		// System.out.println("CopyAgg:"+orig+ " ===> "+orig);
		datastore.setAgg(this, orig.getAgg());
	}

	/** Copy the value from the node pointed to by the cursor */
	void copyVal(NodeCursor<A, V> orig) {
		assert orig.getVal() != null;
		datastore.setVal(this, orig.getVal());
	}
}
