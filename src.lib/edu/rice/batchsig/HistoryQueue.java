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
import java.util.HashMap;
import java.util.Random;


import com.google.protobuf.ByteString;

import edu.rice.batchsig.bench.Tracker;
import edu.rice.historytree.HistoryTree;
import edu.rice.historytree.ProofError;
import edu.rice.historytree.TreeBase;
import edu.rice.historytree.aggs.SHA256Agg;
import edu.rice.historytree.generated.Serialization.PrunedTree;
import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;
import edu.rice.historytree.storage.AppendOnlyArrayStore;
import edu.rice.historytree.storage.HashStore;

/** Process the messages by placing them into a History tree, one for each batch. */
public class HistoryQueue extends QueueBase<OMessage> implements ProcessQueue<OMessage> {
	/** Largest size we want the history tree to grow to before rotating  */
	private final int MAX_SIZE=1<<16 - 2; // Should be just under a power of 2.
	private SignaturePrimitives signer;
	
	/** Track when we last contacted a given recipient_host */
	public HashMap<Object,Integer> lastcontacts;
	/** As a history tree may be used among multiple messages, indicate which message this is dealing with. */
	public long treeid;
	public HistoryTree<byte[], byte[]> histtree;
	
	Object processLock = new Object();
	
	public HistoryQueue(SignaturePrimitives signer) {
		super();
		if (signer == null)
			throw new NullPointerException();
		this.signer = signer;
		initTree();
	}

	private void initTree() {		
		treeid = new Random().nextLong();
		histtree = new HistoryTree<byte[],byte[]>(new SHA256Agg(),new AppendOnlyArrayStore<byte[], byte[]>());
		lastcontacts = new HashMap<Object,Integer>();
	}

	/** Make a new fresh history tree if the additional nodes would make it bigger than the target size. */
	private void rotateStore(int additionalNodes) {
		if (additionalNodes + histtree.version() > MAX_SIZE)
			initTree();
	}
	
	public void process() {
		ArrayList<OMessage> oldqueue = queue.atomicGetQueue();
		if (oldqueue.size() == 0)
			return;
		Tracker.singleton.trackBatchSize(oldqueue.size());

		/**
		 * For now, only a single history tree process can be outstanding. The
		 * current pruned tree building code does not support building pruned
		 * trees around anything but the latest commitment. This means that we
		 * cannot drop the tree lock to do RSA concurrently; the tree will grow
		 * and we'll be unable to generating proofs.
		 *
		 * TODO: If we fix that, then a simple many-readers/one-writers lock will 
		 * allow concurrency between RSA, adding to the history tree and generating proofs.
		 */
		synchronized (processLock) {
			// First, is it big enough to build a new tree?
			rotateStore(oldqueue.size());

			/* Leaf indices are offset by the initial size of the tree */
			int leaf_offset = histtree.version()+1; // Add one because message is inserted at the NEXT index.

			for (Message m : oldqueue) {
				histtree.append(m.getData());
			}

			// Make the unified signature of all.
			TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
					.setTreetype(SigTreeType.HISTORY_TREE)
					.setVersion(histtree.version())
					.setRoothash(ByteString.copyFrom(histtree.agg()));

			// Make the template sigblob containing the RSA signature.
			TreeSigBlob.Builder sigblob = TreeSigBlob.newBuilder();
			sigblob.setSignatureType(SignatureType.SINGLE_HISTORY_TREE);
			signer.sign(msgbuilder.build().toByteArray(),sigblob);

			// Make the read-only template.
			TreeSigBlob template=sigblob.build();

			// Although the tree is semantically read-only, it is still possible
			// for it to be mutated. Eg, during array resizing. A one-writer/many-readers 
			// lock is still needed, but RSA can run in parallel as well as threads generating proofs.
			for (int i = 0; i < oldqueue.size(); i++) {
				processMessage(oldqueue.get(i), leaf_offset + i, TreeSigBlob.newBuilder(template));
			}
			// Update the last contacts for each message.
			for (int i = 0; i < oldqueue.size(); i++) {
				OMessage message = oldqueue.get(i);
				Object recipient = message.getRecipient();
				// Indicate that we want a splicepoint to the end of the bundle.
				lastcontacts.put(recipient,histtree.version());
				
			}
		}
	}

	private void processMessage(OMessage message, int leaf_offset, TreeSigBlob.Builder template) {
		try {
			//System.out.format("Processing leaf %d for recipient host %s\n",leaf_offset, message.getRecipient().toString());
			// Make the pruned tree.
			TreeBase<byte[], byte[]> pruned = histtree
					.makePruned(new HashStore<byte[], byte[]>());
			pruned.copyV(histtree, leaf_offset, true);

			Object recipient = message.getRecipient();
			if (lastcontacts.containsKey(recipient)) {
				//System.out.println(lastcontacts.toString());
				//System.out.println("Found a lastcontact at"+lastcontacts.get(recipient));
				int lastcontact = lastcontacts.get(recipient);
				if (lastcontact != histtree.version()) {
					pruned.copyV(histtree, lastcontacts.get(recipient),false);
					template.addSpliceHint(lastcontacts.get(recipient));
				}
			}
			
			PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
			pruned.serializeTree(treebuilder);

			template
				.setTreeId(treeid)
				.setTree(treebuilder)
				.setLeaf(leaf_offset);
			message.signatureResult(template.build());
		} catch (ProofError e) {
			// Should never occur.
			System.out.println("SHOULDNT OCCUR");
			message.signatureResult(null); // Indicate error.
			e.printStackTrace();
		}
	}
}
