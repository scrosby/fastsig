package org.rice.crosby.batchsig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.rice.crosby.historytree.HistoryTree;
import org.rice.crosby.historytree.ProofError;
import org.rice.crosby.historytree.aggs.SHA256Agg;
import org.rice.crosby.historytree.generated.Serialization.PrunedTree;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.SignatureType;
import org.rice.crosby.historytree.storage.AppendOnlyArrayStore;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.ByteString;

public class HistoryQueue extends QueueBase {
	/** Largest size we want the history tree to grow to before rotating  */
	private final int MAX_SIZE=1000;
	private SignaturePrimitives signer;
	
	/** Track when we last contacted a given recipient */
	public HashMap<Object,Integer> lastcontacts;
	/** As a history tree may be used among multiple messages, indicate which message this is dealing with. */
	public long treeid;
	public HistoryTree<byte[], byte[]> histtree;
	
	public HistoryQueue(SignaturePrimitives signer) {
		super();
		this.signer = signer;
		initTree();
	}

	private void initTree() {		
		treeid = new Random().nextLong();
		histtree = new HistoryTree<byte[],byte[]>(new SHA256Agg(),new AppendOnlyArrayStore<byte[], byte[]>());
		lastcontacts = new HashMap<Object,Integer>();
	}

	private void rotateStore() {
			if (histtree.version() > MAX_SIZE)
				initTree();
	}
	
	public synchronized void process() {
		ArrayList<Message> oldqueue = atomicGetQueue();

		/**
		 * For now, only a single history tree process can be outstanding. The
		 * current pruned tree building code does not support building pruned
		 * trees around anything but the latest commitment. This means that we
		 * cannot drop the tree lock to do RSA concurrently; the tree will grow
		 * and we'll be unable to generating proofs.
		 */
		synchronized (histtree) {

			/* Leaf indices are offset by the initial size of the tree */
			int leaf_offset = histtree.version();

			for (Message m : oldqueue) {
				histtree.append(m.getData());
			}

			// Make the unified signature of all.
			TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
					.setTreetype(SigTreeType.HISTORY_TREE)
					.setVersion(histtree.version())
					.setRoothash(ByteString.copyFrom(histtree.agg()));

			final byte[] rootSig = signer
					.sign(msgbuilder.build().toByteArray());

			// Although the tree is semantically read-only, its still possible
			// for it to be mutated. Eg, during array resizing.
			for (int i = 0; i < oldqueue.size(); i++) {
				processMessage(oldqueue.get(i), leaf_offset + i, rootSig);
			}

			rotateStore();
		}
	}

	private void processMessage(Message message, int leaf_offset, final byte[] rootSig) {
		try {
			TreeSigBlob.Builder blobbuilder = TreeSigBlob.newBuilder();

			// Make the pruned tree.
			HistoryTree<byte[], byte[]> pruned = histtree
					.makePruned(new HashStore<byte[], byte[]>());
			pruned.copyV(histtree, leaf_offset, true);

			Object recipient = message.getRecipient();
			if (lastcontacts.containsKey(recipient)) {
				pruned.copyV(histtree, lastcontacts.get(recipient),false);
				blobbuilder.addSpliceHint(lastcontacts.get(recipient));
			}
			lastcontacts.put(recipient,histtree.version());
			
			PrunedTree.Builder treebuilder = PrunedTree.newBuilder();
			pruned.serializeTree(treebuilder);

			blobbuilder.setSignatureType(SignatureType.SINGLE_HISTORY_TREE)
				.setSignatureBytes(ByteString.copyFrom(rootSig))
				.setTreeId(treeid)
				.setTree(treebuilder)
				.setLeaf(leaf_offset);
			message.signatureResult(blobbuilder.build());
		} catch (ProofError e) {
			// Should never occur.
			message.signatureResult(null); // Indicate error.
			e.printStackTrace();
		}
	}
}
