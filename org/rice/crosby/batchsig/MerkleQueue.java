package org.rice.crosby.batchsig;

import java.util.ArrayList;

import org.rice.crosby.historytree.AggregationInterface;
import org.rice.crosby.historytree.MerkleTree;
import org.rice.crosby.historytree.ProofError;
import org.rice.crosby.historytree.aggs.SHA256Agg;
import org.rice.crosby.historytree.generated.Serialization.PrunedTree;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.TreeType;
import org.rice.crosby.historytree.storage.ArrayStore;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.ByteString;

public class MerkleQueue extends QueueBase {
	private Signer signer;

	public MerkleQueue(Signer signer) {
		super();
		this.signer = signer;
	}


	public void process(Message message) {
		ArrayList<Message> oldqueue = atomicGetQueue();
		
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

		final byte[] rootSig = signer.sign(msgbuilder.build().toByteArray());

		for (int i = 0; i < oldqueue.size(); i++) {
			processMessage(histtree,oldqueue.get(i), i, rootSig);
		}
	}
	private void processMessage(MerkleTree<byte[], byte[]> histtree, Message message, int i, final byte[] rootSig) {	
		try {
			// Make the pruned tree.
			MerkleTree<byte[], byte[]> pruned = histtree
					.makePruned(new HashStore<byte[], byte[]>());
			pruned.copyV(histtree, i, true);

			PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
			pruned.serializeTree(treebuilder);

			TreeSigBlob.Builder blobbuilder = TreeSigBlob.newBuilder()
					.setTreetype(TreeType.SINGLE_MERKLE_TREE).setSig(
							ByteString.copyFrom(rootSig)).setTree(treebuilder)
					.setLeaf(i);
			message.signatureResult(blobbuilder.build());
		} catch (ProofError e) {
			// Should never occur.
			message.signatureResult(null); // Indicate error.
			e.printStackTrace();
		}
	}
}
