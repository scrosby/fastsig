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

public class Verifier {
	private SignaturePrimitives signer;

	public Verifier(SignaturePrimitives signer) {
		this.signer=signer;
	}
	
	/*
	boolean verify(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();
		
		// Other choices are unsupported in this code.
		if (sigblob.getSignatureType() == SignatureType.SINGLE_MERKLE_TREE)
			return verifyMerkle(message);
		else if (sigblob.getSignatureType() == SignatureType.SINGLE_HISTORY_TREE)
			return verifyHistory(message);
		else if (sigblob.getSignatureType() == SignatureType.SINGLE_MESSAGE)
			return verifyMessage(message);
		else 
			return false;

	}
	*/

	boolean verifyMessage(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// Parse the tree.
		final byte[] rootHash = message.getData();
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MESSAGE)
			.setRoothash(ByteString.copyFrom(SimpleQueue.hash(rootHash)));

		return checkSig(sigblob, msgbuilder);
	}

	boolean verifyMerkle(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// Parse the tree.
		MerkleTree<byte[],byte[]> parsed=parseMerkleTree(message);

		// See if the message is in the tree.
		if (!checkLeaf(message, parsed))
			return false;
		
		final byte[] rootHash = parsed.agg();
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MERKLE_TREE)
			.setVersion(parsed.version())
			.setRoothash(ByteString.copyFrom(rootHash));

		return checkSig(sigblob, msgbuilder);
	}

	boolean verifyHistory(Message message) {
		return verifyHistory(message,parseHistoryTree(message));
	}
		
	boolean verifyHistory(Message message, HistoryTree<byte[],byte[]> parsed) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// See if the message is in the tree.
		if (!checkLeaf(message, parsed))
			return false;
		
		final byte[] rootHash = parsed.agg();
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.HISTORY_TREE)
			.setVersion(parsed.version())
			.setRoothash(ByteString.copyFrom(rootHash));

		return checkSig(sigblob, msgbuilder);
	}
		
	static boolean checkLeaf(Message message, TreeBase<byte[], byte[]> parsed) {
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

	boolean checkSig(TreeSigBlob sigblob, TreeSigMessage.Builder msgbuilder) {
		byte[] signeddata = msgbuilder.build().toByteArray();
		return signer.verify(signeddata, sigblob);
	}

	static public MerkleTree<byte[],byte[]> parseMerkleTree(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();
		PrunedTree pb=sigblob.getTree();
		MerkleTree<byte[],byte[]> tree= new MerkleTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree.updateTime(pb.getVersion());
		tree.parseTree(pb);
		return tree;
	}	
	static public HistoryTree<byte[],byte[]> parseHistoryTree(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();
		PrunedTree pb=sigblob.getTree();
		HistoryTree<byte[],byte[]> tree= new HistoryTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree.updateTime(pb.getVersion());
		tree.parseTree(pb);
		return tree;
	}	
}
