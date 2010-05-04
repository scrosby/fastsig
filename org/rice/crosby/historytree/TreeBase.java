package org.rice.crosby.historytree;

public abstract class TreeBase<A,V> {

  /** Make an history at a given timestamp (used as a template for building a pruned trees or parsing trees.)
   */
  public TreeBase<A, V> updateTime(int time) {
  	this.time = time;
  	datastore.updateTime(time);
  	return this;
  }

  protected void reparent(int time) {
  	while (!(time <= (1<<root.layer)-1))
  		this.root = root.reparent();
  }

  public AggregationInterface<A,V> getAggObj() {
  	return aggobj.clone();
  }

  public int version() {
  	return time;
  }

  public String toString(String prefix) {
  	StringBuilder b = new StringBuilder();
  	b.append(prefix);
  	b.append("  version = ");
  	b.append(time);
  	b.append("\n");
  	debugString(b,prefix,root);
  	return new String(b);
  }

  public String toString() {
  	return toString("");
  }

  public void debugString(StringBuilder b, String prefix, NodeCursor<A,V> node) {
  	b.append(prefix);
  	b.append("\t"+toString(node));
  	b.append("\n");
  
  	if (node.isLeaf())
  		return;
  		
  	NodeCursor<A,V> left=node.left(), right=node.right();
  	
  	if (left != null)
  		debugString(b,prefix+"L",left);
  	if (right != null) 
  		debugString(b,prefix+"R",right);
  }

  /** Return this as a longer tab-delimited string */
  public String toString(NodeCursor<A,V> node) {
  	StringBuilder b=new StringBuilder();
  	b.append(String.format("<%d,%d>",node.layer,node.index));
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
  	//b.append(":");
  	//b.append(datastore.hashCode());
  	return b.toString();
  }

  protected String aggToString(A a) {
  	return aggobj.serializeAgg(a).toStringUtf8();
  }

  protected String valToString(V v) {
  	return aggobj.serializeVal(v).toStringUtf8();
  }

  protected int time;
  protected NodeCursor<A,V> root;
  protected HistoryDataStoreInterface<A,V> datastore;
  protected AggregationInterface<A,V> aggobj;

  public static int log2(int x) {
  	int i = 0, pow = 1;
  	while (pow <= x) {
  		pow = pow*2;
  		i=i+1;
  	}
  	return i;
  }

}
