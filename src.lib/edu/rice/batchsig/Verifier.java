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

import java.util.Arrays;


import com.google.protobuf.ByteString;

import edu.rice.historytree.HistoryTree;
import edu.rice.historytree.MerkleTree;
import edu.rice.historytree.NodeCursor;
import edu.rice.historytree.TreeBase;
import edu.rice.historytree.aggs.SHA256Agg;
import edu.rice.historytree.generated.Serialization.PrunedTree;
import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;
import edu.rice.historytree.storage.HashStore;

/**
 * Various common functions useful for verifying Simple, Merkle, and Spliced
 * signatures, and a base class for managing several outstanding signatures.
 */
abstract public class Verifier {
	/** Store the signature primitives used in verifying */
	private SignaturePrimitives signer;

	public Verifier(SignaturePrimitives signer) {
		this.signer = signer;
	}

	/** Within each batch, add this message to be processed. */
	public abstract void add(IMessage message);
	/** At the end of each batch, process the batch. */
	public abstract void process();

	/**
	 * Given a message to be checked, see if the hash stored at that leaf
	 * matches the hash of the message. Shared for both Merkle and history
	 * trees.
	 */
	static public boolean checkLeaf(IMessage message, TreeBase<byte[], byte[]> parsed) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// See if the message is in the tree.
		NodeCursor<byte[], byte[]> leaf = parsed.leaf(sigblob.getLeaf());
		if (leaf == null)
			return false;
		byte [] leafagg = leaf.getAgg();
		byte [] msgagg = parsed.getAggObj().aggVal(message.getData());

		
		if (! Arrays.equals(msgagg,leafagg))
			// Nope, we fail.
			return false;
		return true;
	}

	/** Verify the public key signature. 
	 * 
	 * @return true if the signature matches. 
	 */
	protected boolean checkSig(TreeSigBlob sigblob, TreeSigMessage.Builder msgbuilder) {
		byte[] signeddata = msgbuilder.build().toByteArray();
		return signer.verify(signeddata, sigblob);
	}
}
