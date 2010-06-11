package org.rice.crosby.batchsig;

import java.util.ArrayList;

import org.rice.crosby.historytree.AggregationInterface;
import org.rice.crosby.historytree.MerkleTree;
import org.rice.crosby.historytree.ProofError;
import org.rice.crosby.historytree.TreeBase;
import org.rice.crosby.historytree.aggs.SHA256Agg;
import org.rice.crosby.historytree.generated.Serialization.PrunedTree;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.SignatureType;
import org.rice.crosby.historytree.storage.ArrayStore;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.ByteString;

/** Process the messages by placing them into a Merkle, one for each batch. */
public class MerkleQueue extends QueueBase {
	private SignaturePrimitives signer;

	public MerkleQueue(SignaturePrimitives signer) {
		super();
		this.signer = signer;
	}


	public void process() {
		ArrayList<Message> oldqueue = atomicGetQueue();
		if (oldqueue.size() == 0)
			return;
		
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
