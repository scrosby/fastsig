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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import edu.rice.historytree.HistoryTree;
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

/**
 * Top level queue for verifying incoming messages. Works eagerly, verifying all
 * outstanding messages on each invocation.
 */
public class VerifyQueue extends QueueBase<IMessage> implements ProcessQueue<IMessage> {
	private Verifier verifier;
	private SignaturePrimitives signer;
	private VerifyMerkle merkleverifier;
	private VerifyAtomicSignature atomicverifier;
	private VerifyHisttree histtreeverifier;
	
	public VerifyQueue(SignaturePrimitives signer) {
		super();
		if (signer == null)
			throw new NullPointerException();
		this.signer = signer;
		this.merkleverifier = new VerifyMerkle(signer);
		this.atomicverifier = new VerifyAtomicSignature(signer);
		this.histtreeverifier = new VerifyHisttreeGroup(signer);
	}
	
	public void process() {
		ArrayList<IMessage> oldqueue = atomicGetQueue();


		// Go over each message
		for (IMessage m : oldqueue) {
			if (m == null) {
				System.err.println("Null message in queue?");
				continue;
			}
			TreeSigBlob sigblob = m.getSignatureBlob();
			if (sigblob.getSignatureType() == SignatureType.SINGLE_MESSAGE) {
				// If it is a singlely signed message, check.
				// TODO: Do concurrently; dispatch into thread pool.
				atomicverifier.add(m);
			} else if (sigblob.getSignatureType() == SignatureType.SINGLE_MERKLE_TREE) {
				// If its is a merkle tree message, check.
				// TODO: Do concurrently; dispatch into thread pool.
				merkleverifier.add(m);
			} else if (sigblob.getSignatureType() == SignatureType.SINGLE_HISTORY_TREE) {
				histtreeverifier.add(m);
				// If a history tree, put into a set of queues, one for each signer.
			} else {
				System.out.println("Unrecognized SignatureType");
			}
		}
		atomicverifier.finishBatch();
		merkleverifier.finishBatch();
		histtreeverifier.finishBatch();
	}
}

