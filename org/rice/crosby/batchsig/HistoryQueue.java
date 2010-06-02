package org.rice.crosby.batchsig;

import java.util.ArrayList;

import org.rice.crosby.historytree.AggregationInterface;
import org.rice.crosby.historytree.HistoryTree;
import org.rice.crosby.historytree.MerkleTree;
import org.rice.crosby.historytree.ProofError;
import org.rice.crosby.historytree.aggs.SHA256Agg;
import org.rice.crosby.historytree.generated.Serialization.PrunedTree;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.TreeType;
import org.rice.crosby.historytree.storage.AppendOnlyArrayStore;
import org.rice.crosby.historytree.storage.ArrayStore;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.ByteString;

public class HistoryQueue extends QueueBase {
	private Signer signer;
	
	/** As a history tree may be used among multiple messages, indicate which message this is dealing with. */
	long treeid;
	HistoryTree<byte[], byte[]> tree;
	
	public HistoryQueue(Signer signer) {
		super();
		this.signer = signer;
		initTree();
	}

	private void initTree() {
		//treeid = RandomLong();
		tree = new HistoryTree<byte[],byte[]>(new SHA256Agg(),new AppendOnlyArrayStore<byte[], byte[]>());
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
			try {
				// Make the pruned tree.
				MerkleTree<byte[], byte[]> pruned = histtree
						.makePruned(new HashStore<byte[], byte[]>());
				pruned.copyV(histtree, i, true);

				PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
				pruned.serializeTree(treebuilder);

				TreeSigBlob.Builder blobbuilder = TreeSigBlob.newBuilder()
				.setTreetype(TreeType.SINGLE_HISTORY_TREE)
				.setSig(ByteString.copyFrom(rootSig))
				.setTreeId(treeid)
				.setTree(treebuilder)
				.setLeaf(i);
			} catch (ProofError e) {
				// Should never occur.
				oldqueue.get(i).signatureResult(null); // Indicate error.
				e.printStackTrace();
			}
		}
	}
}
