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

package edu.rice.batchsig;

import java.util.ArrayList;


import com.google.protobuf.ByteString;

import edu.rice.batchsig.bench.Tracker;
import edu.rice.historytree.AggregationInterface;
import edu.rice.historytree.MerkleTree;
import edu.rice.historytree.ProofError;
import edu.rice.historytree.TreeBase;
import edu.rice.historytree.aggs.SHA256Agg;
import edu.rice.historytree.generated.Serialization.PrunedTree;
import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;
import edu.rice.historytree.storage.ArrayStore;
import edu.rice.historytree.storage.HashStore;

/** Sign a set of messages by placing them into a history tree.
 * 
 * A new merkle tree is used for each batch.
 */
public class MerkleQueue extends QueueBase<OMessage> implements SuspendableProcessQueue<OMessage> {
	public MerkleQueue(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	public void process() {
		ArrayList<OMessage> oldqueue = atomicGetQueue();
		if (oldqueue.size() == 0)
			return;

		Tracker.singleton.trackBatchSize(oldqueue.size());
		
		AggregationInterface<byte[], byte[]> aggobj = new SHA256Agg();
		ArrayStore<byte[], byte[]> datastore = new ArrayStore<byte[], byte[]>();
		MerkleTree<byte[], byte[]> merkletree = new MerkleTree<byte[], byte[]>(
				aggobj, datastore);

		// Add all of the messages to the Merkle tree.
		for (Message m : oldqueue) {
			merkletree.append(m.getData());
		}
		merkletree.freeze();

		// At this point, everything is read-only. I can generate signatures and
		// pruned trees concurrently.
		// 
		// The only data-dependency is on rootSig; I need to sign before I can
		// generate the output messages.

		final byte[] rootHash = merkletree.agg();

		// Make the unified signature of all.
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MERKLE_TREE)
			.setVersion(merkletree.version())
			.setRoothash(ByteString.copyFrom(rootHash));

		// Make the template sigblob containing the RSA signature.
		TreeSigBlob.Builder sigblob = TreeSigBlob.newBuilder();
		sigblob.setSignatureType(SignatureType.SINGLE_MERKLE_TREE);
		signer.sign(msgbuilder.build().toByteArray(), sigblob);

		// Make the read-only template.
		TreeSigBlob template = sigblob.build();
				
		for (int i = 0; i < oldqueue.size(); i++) {
			processMessage(merkletree, oldqueue.get(i), i, TreeSigBlob.newBuilder(template));
		}
	}
	
	/** Generate the pruned tree field for each message, given the template 
	 * containing the public key signature. */
	private void processMessage(TreeBase<byte[], byte[]> merkletree, OMessage message, int leaf, 
			TreeSigBlob.Builder template) {	
		try {
			// Make the pruned tree.
			TreeBase<byte[], byte[]> pruned = merkletree
					.makePruned(new HashStore<byte[], byte[]>());
			pruned.copyV(merkletree, leaf, true);

			PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
			pruned.serializeTree(treebuilder);

			template
					.setTree(treebuilder)
					.setLeaf(leaf);
			message.signatureResult(template.build());
		} catch (ProofError e) {
			// Should never occur.
			message.signatureResult(null); // Indicate error.
			e.printStackTrace();
		}
	}
}
