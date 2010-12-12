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
 * outstanding messages on each invocation. Wraps the various Verifier
 * implementations into a ProcessQueue. Demultiplexes messages that have Merkle
 * signatures, simple signatures, or spliced signatures and calls the
 * appropriate underlying verifier.
 */
public class VerifyQueue extends QueueBase<IMessage> implements SuspendableProcessQueue<IMessage> {
	/** The verifier used to handle Merkle signatures */
	private VerifyMerkle merkleverifier;
	/** The verifier used to handle simple signatures. */
	private VerifySimple atomicverifier;
	/** The verifier used to handle spliced signatures */
	private VerifyHisttreeEagerlyBase histtreeverifier;
	
	public VerifyQueue(SignaturePrimitives signer) {
		super(signer);
		this.merkleverifier = new VerifyMerkle(signer);
		this.atomicverifier = new VerifySimple(signer);
		this.histtreeverifier = new VerifyHisttreeGroup(signer);
	}
	
	@Override
	public void process() {
		ArrayList<IMessage> oldqueue = atomicGetQueue();

		// Go over each message
		for (IMessage m : oldqueue) {
			if (m == null) {
				System.err.println("Null message in queue?");
				continue;
			}
			TreeSigBlob sigblob = m.getSignatureBlob();

			// Dispatch based on the type. 
			if (sigblob.getSignatureType() == SignatureType.SINGLE_MESSAGE) {
				atomicverifier.add(m);
			} else if (sigblob.getSignatureType() == SignatureType.SINGLE_MERKLE_TREE) {
				merkleverifier.add(m);
			} else if (sigblob.getSignatureType() == SignatureType.SINGLE_HISTORY_TREE) {
				histtreeverifier.add(m);
			} else {
				System.out.println("Unrecognized SignatureType");
			}
		}
		atomicverifier.process();
		merkleverifier.process();
		histtreeverifier.process();
	}
}
