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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import edu.rice.historytree.HistoryDataStoreInterface;
import edu.rice.historytree.NodeCursor;


public class FastUtilsHashStore<A,V> extends StoreBase implements HistoryDataStoreInterface<A, V> {
	private Int2ObjectMap<A>  aggstore;
	private int time;
	private Int2ObjectMap<V>  valstore;

	public FastUtilsHashStore() {
		this.time = -1;
		this.aggstore = new Int2ObjectOpenHashMap<A>();
		this.valstore = new Int2ObjectOpenHashMap<V>();
	}
	@Override
	public NodeCursor<A, V> makeRoot(int layer) {
		return new NodeCursor<A,V>(this,layer,0);
	}

	@Override
	public A getAgg(NodeCursor<A, V> node) {
		Integer key=new Integer(node.computeIndex());
		//System.out.println("GetAgg "+key+"["+"]"+aggstore.get(key));
		return aggstore.get(key); 
		}

	@Override
	public V getVal(NodeCursor<A, V> node) {
		return valstore.get(new Integer(node.index()));
	}

	@Override
	public boolean hasVal(NodeCursor<A, V> node) {
		return valstore.get(new Integer(node.index())) != null;
	}

	@Override
	public boolean isAggValid(NodeCursor<A, V> node) {
		return aggstore.containsKey(new Integer(node.computeIndex()));
	}

	@Override
	public void markValid(NodeCursor<A, V> node) {
		Integer key=new Integer(node.computeIndex());
		if (!aggstore.containsKey(key))
			aggstore.put(key,null);
	}

	@Override
	public void setAgg(NodeCursor<A, V> node, A a) {
		assert(isAggValid(node));
		Integer key=new Integer(node.computeIndex());
		//System.out.println("SetAgg "+key+"["+node+"] = "+a);
		aggstore.put(key,a);
	}

	@Override
	public void setVal(NodeCursor<A, V> node, V v) {
		// Also, vals cannot be primitive types. Need a 'null' to indicate invalid.
		assert (v != null);
		valstore.put(new Integer(node.index()),v);
	}

	@Override
	public void updateTime(int time) {
		assert (time >= this.time);
		this.time = time;		
	}
}


