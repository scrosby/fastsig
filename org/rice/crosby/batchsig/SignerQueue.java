package org.rice.crosby.batchsig;

import java.util.ArrayList;

import org.rice.crosby.historytree.AggregationInterface;
import org.rice.crosby.historytree.MerkleTree;
import org.rice.crosby.historytree.ProofError;
import org.rice.crosby.historytree.aggs.SHA256Agg;
import org.rice.crosby.historytree.generated.Serialization.BlobConfig;
import org.rice.crosby.historytree.generated.Serialization.PrunedTree;
import org.rice.crosby.historytree.generated.Serialization.SigConfig;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.TreeType;
import org.rice.crosby.historytree.storage.ArrayStore;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.ByteString;

public class SignerQueue {
	private ArrayList<Message> queue;
	private Signer signer;
	final SigConfig sigconfig;
	final BlobConfig blobconfig;

	public SignerQueue(Signer signer) {
		this.signer=signer;
		initQueue();
		sigconfig = SigConfig.newBuilder().setTreetype(SigTreeType.MERKLE_TREE).build();
		blobconfig = BlobConfig.newBuilder().setTreetype(TreeType.SINGLE_MERKLE_TREE).build();
	}

	private void initQueue() {
		queue = new ArrayList<Message>(32);
	}
	
	public void add(Message message) {
		synchronized(this) {
			queue.add(message);
		}
	}

	/** Process all of the messages, signing every one. May be done in a separate signing thread */
	void process(Message message) {
		ArrayList<Message> oldqueue;
		synchronized(this) {
			oldqueue = queue;
			initQueue();
		}
		
		AggregationInterface<byte[],byte[]> aggobj = new SHA256Agg();
		ArrayStore<byte[],byte[]> datastore = new ArrayStore<byte[],byte[]>();
		MerkleTree<byte[],byte[]> histtree = new MerkleTree<byte[],byte[]>(aggobj,datastore);

		
		for (Message m : oldqueue) {
			histtree.append(m.getData());
		}
		histtree.freeze();

		// At this point, everything is read-only. I can generate signatures and pruned trees concurrently.
		// 
		// The only data-dependency is on rootSig; I need to sign before I can generate the output messages.
		
		final byte[] rootHash = histtree.agg();

		// Make the unified signature of all.
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder();
		msgbuilder.setConfig(sigconfig);
		msgbuilder.setVersion(histtree.version());
		msgbuilder.setRoothash(ByteString.copyFrom(rootHash));
		final byte[] rootSig = signer.sign(msgbuilder.build().toByteArray());
		
		for (int i = 0 ; i < oldqueue.size(); i++) {
			try {
				// Make the pruned tree.
				MerkleTree<byte[],byte[]> pruned = histtree.makePruned(new HashStore<byte[],byte[]>());
				pruned.copyV(histtree, i, true);

				PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
				pruned.serializeTree(treebuilder);
				
				TreeSigBlob.Builder blobbuilder = TreeSigBlob.newBuilder();
				blobbuilder.setConfig(blobconfig);
				blobbuilder.setSig(ByteString.copyFrom(rootSig));
				blobbuilder.setTree(treebuilder);
				blobbuilder.setLeaf(i);
				oldqueue.get(i).signatureResult(blobbuilder.build());
			} catch (ProofError e) {
				// Should never occur.
				oldqueue.get(i).signatureResult(null); // Indicate error.
				e.printStackTrace();
			}
		}
	}
}
