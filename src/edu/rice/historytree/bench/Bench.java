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

package edu.rice.historytree.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;


import com.google.protobuf.InvalidProtocolBufferException;

import edu.rice.historytree.AggregationInterface;
import edu.rice.historytree.HistoryDataStoreInterface;
import edu.rice.historytree.HistoryTree;
import edu.rice.historytree.MerkleTree;
import edu.rice.historytree.NodeCursor;
import edu.rice.historytree.ProofError;
import edu.rice.historytree.TreeBase;
import edu.rice.historytree.aggs.SHA256Agg;
import edu.rice.historytree.generated.Serialization.PrunedTree;
import edu.rice.historytree.storage.AppendOnlyArrayStore;
import edu.rice.historytree.storage.ArrayStore;
import edu.rice.historytree.storage.HashStore;

import bb.util.Benchmark;

public class Bench {

	ArrayList<byte[]> makeKeyList(int keycount) {
		ArrayList<byte[]> out = new ArrayList<byte[]>(keycount+1);
		for (int j =0; j < keycount ; j++) {
			out.add(String.format("Foo%d",j).getBytes());
		}
		return out;
	}
	
	
	TreeBase<byte[], byte[]> makeMerkleTree(List<byte[]> keys, HistoryDataStoreInterface<byte[], byte[]> datastore) {
		AggregationInterface<byte[],byte[]> aggobj = new SHA256Agg();
		MerkleTree<byte[],byte[]> histtree=new MerkleTree<byte[],byte[]>(aggobj,datastore);
		for (byte[] key : keys) {
			histtree.append(key);
		}
		histtree.freeze();
		return histtree;
	}

	TreeBase<byte[], byte[]>  makeHistoryTree(List<byte[]> keys, HistoryDataStoreInterface<byte[], byte[]> datastore) {
		AggregationInterface<byte[],byte[]> aggobj = new SHA256Agg();
		HistoryTree<byte[],byte[]> histtree=new HistoryTree<byte[],byte[]>(aggobj,datastore);
		for (byte[] key : keys)
			histtree.append(key);
		return histtree;
	}

	Callable<TreeBase<byte[], byte[]>> makeTree1(final List<byte[]> keys) {
			return new Callable<TreeBase<byte[], byte[]>>() {
				public TreeBase<byte[], byte[]> call() {return makeHistoryTree(keys,new ArrayStore<byte[],byte[]>());
			}
		};
	}	

	Callable<TreeBase<byte[], byte[]>> makeTree2(final List<byte[]> keys) {
		return new Callable<TreeBase<byte[], byte[]>>() {
			public TreeBase<byte[], byte[]> call() {return makeHistoryTree(keys,new AppendOnlyArrayStore<byte[],byte[]>());
			}
		};
	}

	Callable<TreeBase<byte[], byte[]>> makeTree3(final List<byte[]> keys) {
		return new Callable<TreeBase<byte[], byte[]>>() {
			public TreeBase<byte[], byte[]> call() {return makeHistoryTree(keys,new HashStore<byte[],byte[]>());
			}
		};
	}
	Callable<TreeBase<byte[], byte[]>> makeTree4(final List<byte[]> keys) {
		return new Callable<TreeBase<byte[], byte[]>>() {
			public TreeBase<byte[], byte[]> call() {return makeMerkleTree(keys,new ArrayStore<byte[],byte[]>());
			}
		};
	}

	Callable<TreeBase<byte[], byte[]>> makeTree5(final List<byte[]> keys) {
		return new Callable<TreeBase<byte[], byte[]>>() {
			public TreeBase<byte[], byte[]> call() {return makeMerkleTree(keys,new HashStore<byte[],byte[]>());
			}
		};
	}
	
	class Proof {
		public Proof(byte[] blob, int leaf) {
			this.blob = blob;
			this.leaf = leaf;
		}
		final public byte[] blob;
		final public int leaf;
	}
	
	
	Callable<TreeBase<byte[], byte[]>> makeProof(final TreeBase<byte[], byte[]> tree) {
		final Random rand = new Random();
		
		return new Callable<TreeBase<byte[], byte[]>>() {
			public TreeBase<byte[], byte[]> call() {
				TreeBase<byte[],byte[]> clone=tree.makePruned(new HashStore<byte[],byte[]>());		
				try {
					clone.copyV(tree,rand.nextInt(tree.version()+1),false);
				} catch (ProofError e) {
					e.printStackTrace();
				}
				return clone;
				}
		};
	}
	Callable<Proof> makeSerializedProof(final TreeBase<byte[], byte[]> tree) {
		final Random rand = new Random();
			
		return new Callable<Proof>() {
			public Proof call() {
				TreeBase<byte[],byte[]> clone=tree.makePruned(new HashStore<byte[],byte[]>());		
				int i = rand.nextInt(tree.version()+1);
				try {
					clone.copyV(tree,i,false);
						} catch (ProofError e) {
							e.printStackTrace();
					}
					return new Proof(clone.serializeTree(),i);
					}
			};
	}

    Callable<Boolean> verifyProofs(final List<Proof> proofs) {
    	return new Callable<Boolean>() {
    		public Boolean call() {
    			boolean alltrue = true;
    			for (Proof proof : proofs) {
    				PrunedTree.Builder builder = PrunedTree.newBuilder();
    				PrunedTree pb;
    				try {
    					pb = builder.mergeFrom(proof.blob).build();
    					HistoryTree<byte[],byte[]> tree2= new HistoryTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
    					tree2.updateTime(pb.getVersion());
    					tree2.parseTree(pb);
    					NodeCursor<byte[],byte[]> leaf = tree2.leaf(proof.leaf);
    					if (leaf == null)
    						alltrue = false;
    				} catch (InvalidProtocolBufferException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}

    			}
    			return alltrue;
    		}
    	};
    }

    public void estimateSizes(String prefix, Callable<TreeBase<byte[], byte[]>> treec) {
    	try {
    	long bytes = 0, count = 0;
    	for (int i=0 ; i < 10 ; i++) {
    		Callable<Proof> maker = makeSerializedProof(treec.call());
    		for (int j=0 ; j < 1000 ; j++) {
    			count++;
    			bytes += maker.call().blob.length;
    		}
    	}	
    	System.out.format("%s-Prooflen: = %d\n",prefix,bytes/count);
    	} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
       	}
    }
    
	
	public void doBenchmark(int mode, int keycount) throws Exception {
		ArrayList<byte[]> keys = makeKeyList(keycount);
		Callable<TreeBase<byte[], byte[]>> treec = null;
		String prefix = "";
		if (mode == 1) {
			treec = makeTree1(keys);  prefix="Tree-HA-";
		} else if (mode == 2) {
			treec = makeTree2(keys);  prefix="Tree-HAA-";
		} else if (mode == 3) {
			treec = makeTree3(keys);  prefix="Tree-HH-";
		} else if (mode == 4) {
			treec = makeTree4(keys);  prefix="Tree-MA-";
		} else if (mode == 5) {
			treec = makeTree5(keys);  prefix="Tree-MH-";
		} else
			throw new Error();

		makeSerializedProof(treec.call()).call();

		estimateSizes(prefix, treec);
		//if (true)
		//	return;
		
		System.out.println(prefix+"Build " + new Benchmark(treec));
		TreeBase<byte[], byte[]> tree = treec.call();
		System.out.println(prefix+"ProofGen " + new Benchmark(makeProof(tree)));
		System.out.println(prefix+"ProofString " + new Benchmark(makeSerializedProof(tree)));

		int COUNT = 1000;
		ArrayList<Proof> proofs = new ArrayList<Proof>(COUNT+1);
		Callable<Proof> maker = makeSerializedProof(tree);
		for (int i = 0 ; i < COUNT ; i++)
			proofs.add(maker.call());
		System.out.println(prefix+"Verify " + new Benchmark(verifyProofs(proofs)));

		
	
	}
	
	public static void main(String[] args) {
		Bench self = new Bench();
		
		try {
			self.doBenchmark(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}	
}
