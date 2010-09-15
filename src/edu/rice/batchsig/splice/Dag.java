package edu.rice.batchsig.splice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Dag<T> {
	/** Map from a descriptor to the DagNode containing it. */
	final HashMap<T,DagNode> nodeMap = new HashMap<T,DagNode>(); 
	
	DagNode makeOrGet(T key) {
		if (nodeMap.containsKey(key))
			return nodeMap.get(key);
		DagNode out = new DagNode(key);
		nodeMap.put(key,out);
		return out;
	}
	
	/** Represent the edges: Multimap from a node to the set of its parents. */
	final Multimap<DagNode,DagNode> parents = HashMultimap.create();
	/** Represent the edges: Multimap from a node to the set of its children. */
	final Multimap<DagNode,DagNode> children = HashMultimap.create();

	public class DagNode {
		/** Two dagnodes created with the same object are considered distinct. */
		public DagNode(T object) {
			this.object = object;
		}
		T object;
		
		public T get() {return object;}
		
		Collection<DagNode> getParents() {
			return parents.get(this);
		}
		Collection<DagNode> getChildren() {
			return children.get(this);
		}

		void remove() {
			Collection<DagNode> children = getChildren();
			Collection<DagNode> parents = getParents();
			for (DagNode i : children)
				removeEdge(this,i);
			for (DagNode i : parents) 
				removeEdge(i,this);
		}
			
		DagNode getAParent() {
			Collection<DagNode> tmp = getParents();
			if (tmp.isEmpty())
				return null;
			else 
				return tmp.iterator().next();
		}

		public Iterator<T> getParentIterator() {
			final Iterator<DagNode> i = getParents().iterator();
			return new Iterator<T>(){
				public boolean hasNext() {return i.hasNext();}
				public T next() {return i.next().object;}
				public void remove() {throw new UnsupportedOperationException();}
			};
		}
	
	}

	class Path {
		ArrayList<DagNode> path = new ArrayList<DagNode>();

		public DagNode root() {
			return path.get(path.size()-1);
		}

		void removeLast() {
			path.remove(path.size()-1);
		}
		
		void extend() {
			DagNode node=root(), parent;
			while ((parent = node.getAParent()) != null) {
				node = parent;
				path.add(node);	
		}
		}
		public void next() {
			removeLast();
			extend();
		}
	}
	
	Path rootPath(DagNode node) {
		Path out = new Path();
		out.path.add(node);
		out.extend();
		return out;
	}
			

	
	void addEdge(DagNode parent, DagNode child) {
		if (children.get(parent).contains(child))
			throw new Error("Storing a duplicate edge");
		if (parents.get(child).contains(parent)) // Should never trigger unless the graph inconsistent.
			throw new Error("Storing a duplicate edge");
		children.put(parent,child);
		parents.put(child,parent);
	}
	void removeEdge(DagNode parent, DagNode child) {
		if (!children.get(parent).contains(child))
			throw new Error("Removing non-existant edge");
		if (!parents.get(child).contains(parent)) // Should never trigger unless the graph inconsistent.
			throw new Error("Removing non-existant edge");
		children.remove(parent,child);
		parents.remove(child,parent);
	}
	
	Collection<DagNode> getAllChildren(DagNode node) {
		HashSet<DagNode> todo = new HashSet<DagNode>();
		HashSet<DagNode> out = new HashSet<DagNode>();
		todo.add(node);
		
		while (todo.size() > 0) {
			HashSet<DagNode> unvisited = new HashSet<DagNode>();
			for (DagNode i : unvisited) {
				for (DagNode j : i.getChildren()) {
					if (!out.contains(j)) {
						out.add(j);
						unvisited.add(j);
					}
				}
			}
			todo = unvisited;
		}
		return out;
	}
}