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

package edu.rice.historytree.storage;

import java.util.ArrayList;

import edu.rice.historytree.NodeCursor;

/** An array store where some interior nodes may not be valid.
 */

public class ArrayStore<A,V> extends ArrayStoreBase<A, V> 
		 {

	public ArrayStore() {
		super();
		this.aggvalid = new ArrayList<Boolean>(5);
	}

	@Override
	public boolean isAggValid(NodeCursor<A, V> node) {
		return aggvalid.get(node.computeIndex()).booleanValue();
	}

	@Override
	public void markValid(NodeCursor<A, V> node) {
		assert node.index() <= time : "Prob: "+ node.index() + " " +time;
		aggvalid.set(node.computeIndex(), new Boolean(true));
	}

	@Override
	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;		

		
		while (time+1+1 > valstore.size()) // An extra +1 to handle hasVal's on extra nodes with emptyVal's inserted into a merkle tree.
			valstore.add(null);
		while (4*time+1 > aggstore.size()) {
			aggstore.add(null);
			aggvalid.add(new Boolean(false));
		}
	}

	/** Record whether a given agg is valid. Offset into the array is the node's index in a post order traversal. */
	private ArrayList<Boolean>  aggvalid;

}


