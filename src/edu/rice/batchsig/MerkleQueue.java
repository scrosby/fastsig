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

/** Process the messages by placing them into a Merkle, one for each batch. */
public class MerkleQueue extends QueueBase {
	private SignaturePrimitives signer;

	public MerkleQueue(SignaturePrimitives signer) {
		super();
		if (signer == null)
			throw new NullPointerException();
		this.signer = signer;
	}


	public void process() {
		ArrayList<Message> oldqueue = atomicGetQueue();
		if (oldqueue.size() == 0)
			return;

		Tracker.singleton.trackBatchSize(oldqueue.size());
		
		AggregationInterface<byte[], byte[]> aggobj = new SHA256Agg();
		ArrayStore<byte[], byte[]> datastore = new ArrayStore<byte[], byte[]>();
		MerkleTree<byte[], byte[]> histtree = new MerkleTree<byte[], byte[]>(
				aggobj, datastore);
		
		for (Message m : oldqueue) {
			histtree.append(m.getData());
		}
		histtree.freeze();

		// At this point, everything is read-only. I can generate signatures and
		// pruned trees concurrently.
		// 
		// The only data-dependency is on rootSig; I need to sign before I can
		// generate the output messages.

		final byte[] rootHash = histtree.agg();

		// Make the unified signature of all.
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MERKLE_TREE)
			.setVersion(histtree.version())
			.setRoothash(ByteString.copyFrom(rootHash));

		// Make the template sigblob containing the RSA signature.
		TreeSigBlob.Builder sigblob = TreeSigBlob.newBuilder();
		sigblob.setSignatureType(SignatureType.SINGLE_MERKLE_TREE);
		signer.sign(msgbuilder.build().toByteArray(),sigblob);

		// Make the read-only template.
		TreeSigBlob template=sigblob.build();
				
		for (int i = 0; i < oldqueue.size(); i++) {
			processMessage(histtree,oldqueue.get(i), i, TreeSigBlob.newBuilder(template));
		}
	}
	private void processMessage(TreeBase<byte[], byte[]> histtree, Message message, int i, TreeSigBlob.Builder template) {	
		try {
			// Make the pruned tree.
			TreeBase<byte[], byte[]> pruned = histtree
					.makePruned(new HashStore<byte[], byte[]>());
			pruned.copyV(histtree, i, true);

			PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
			pruned.serializeTree(treebuilder);

			template
					.setTree(treebuilder)
					.setLeaf(i);
			message.signatureResult(template.build());
		} catch (ProofError e) {
			// Should never occur.
			message.signatureResult(null); // Indicate error.
			e.printStackTrace();
		}
	}
}
