package org.rice.crosby.historytree;


public class NodeCursor<A,V> {
	interface NodeFactoryInterface<A,V> {
		/** A node cursor can point anywhere. This indicates that we should create the references cursor location. */
		void markValid(NodeCursor<A,V> node);
		/** A node cursor can point anywhere. This sees if we do in fact have valid data at the cursor location */
		boolean isAggValid(NodeCursor<A,V> node);

		void setAgg(NodeCursor<A,V> node, A a);
		void setVal(NodeCursor<A,V> node, V v);
		A getAgg(NodeCursor<A,V> node);
		V getVal(NodeCursor<A,V> node);
		boolean hasVal(NodeCursor<A,V> node);
	};
	
	
	NodeCursor(int layer, int index) {
		this.nodefactory = null;
		this.layer = layer;
		this.index = index;
	}
	NodeCursor(NodeFactoryInterface<A,V> nodefactory, int layer, int index) {
		this.nodefactory = nodefactory;
		this.layer = layer;
		this.index = index;
	}
	
	/*
	 * Helper functions 
	*/
	
	boolean isFrozen(int time) {
		return time >= index+getStep()-1;
	}
	int getStep() {
		return 1 << layer;
	}
	boolean isLeaf() {
		return layer == 0;
	}

	/** Compute index in a total order */
	protected int computeIndex() {
		int s = 0;
		int j = index + (1<<layer)-1;
		while (j>0) {
			s = s+j;
			j = j/2;
		}
		return s+layer;
	}

	protected final NodeFactoryInterface<A,V> nodefactory;
	protected final int layer;
	protected final int index;

	/** Return a NodeCursor reference with the layer and index numbers of
	 * the given child. May or may not actually exist. */
	NodeCursor<A,V> getLeft() {
		int newindex = index;
		return new NodeCursor<A,V>(nodefactory,layer-1,newindex);
	}
	/** Return a NodeCursor reference with the layer and index numbers of
	 * the given child. May or may not actually exist. */
	NodeCursor<A,V> getRight() {
		int newindex = index + getStep()/2;
		return new NodeCursor<A,V>(nodefactory,layer-1,newindex);
	}

	NodeCursor<A,V> reparent() {
		return new NodeCursor<A,V>(nodefactory,layer+1,0).markValid();
	}

	/** Get the parent node of the current cursor. In order to return 'null' for the root node, 
	 * to indicate that it is not the parent, we must know what the root is. */
	NodeCursor<A,V> getParent(final NodeCursor<A,V> root) {
		if (this.equals(root))
			return null;
		return new NodeCursor<A,V>(nodefactory,layer+1,index & ~(getStep()*2-1));
	}

	NodeCursor<A,V> left() {
		NodeCursor<A,V> out=getLeft();
		if (out.isAggValid())
			return out;
		else
			return null;
	}

	NodeCursor<A,V> right() {
		NodeCursor<A,V> out=getRight();
		if (out.isAggValid())
			return out;
		else
			return null;
	};

	/** Return the layer and index numbers of the given child. Create
	 * if needed. */
	NodeCursor<A,V> forceLeft() {
		return getLeft().markValid();
	}
	/** Return the layer and index numbers of the given child. Create
	 * if needed. */
	NodeCursor<A,V> forceRight() {
		return getRight().markValid();
	}

	public boolean equals(Object o) {
		if (!(o instanceof NodeCursor<?,?>)) return false;
		NodeCursor<A,V> o2 = (NodeCursor<A,V>)o;
		if (this.nodefactory != o2.nodefactory) return false;
		if (this.layer != o2.layer) return false;
		if (this.index != o2.index) return false;
		return true;
	}

	public String toString() {
		return "TODO: NodeCursor<A,V>Common.toString()";
	}

	/*
	 * Functions that reflect up to the nodefactory.
	 */
	NodeCursor<A,V> markValid() {
		nodefactory.markValid(this);
		return this;
	}
	boolean isAggValid() {
		return nodefactory.isAggValid(this);
	}
	boolean hasVal() {
		return nodefactory.hasVal(this);
	}
	V getVal() {
		return nodefactory.getVal(this);
	}
	void setVal(V v) {
		nodefactory.setVal(this,v);
	}
	A getAgg() {
		return nodefactory.getAgg(this);
	}
	void setAgg(A v) {
		nodefactory.setAgg(this,v);
	}
	void copyAgg(NodeCursor<A,V> orig) {
		nodefactory.setAgg(this,orig.getAgg());
	}
	void copyVal(NodeCursor<A,V> orig) {
		nodefactory.setVal(this,orig.getVal());
	}
}
