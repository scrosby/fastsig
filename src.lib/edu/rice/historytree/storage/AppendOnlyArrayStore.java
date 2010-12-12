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

import edu.rice.historytree.NodeCursor;

/**
 * An array store intended only for append-only operation with the history tree.
 * It implicitly assumes and requires that all nodes up to the last one are
 * always valid, and none after that are valid.
 */
public class AppendOnlyArrayStore<A, V> extends ArrayStoreBase<A, V> {

	public AppendOnlyArrayStore() {
		super();
	}

	@Override
	public boolean isAggValid(NodeCursor<A, V> node) {
		return node.index() <= time;
	}

	@Override
	public void markValid(NodeCursor<A, V> node) {
		assert (node.index() <= time);
	}

	@Override
	public void updateTime(int time) {
		assert (time > this.time);
		this.time = time;

		while (time + 1 > valstore.size())
			valstore.add(null);
		while (2 * time + 1 > aggstore.size())
			aggstore.add(null);
	}

}
