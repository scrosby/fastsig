package edu.rice.batchsig;

import com.google.protobuf.ByteString;

import edu.rice.historytree.HistoryTree;
import edu.rice.historytree.aggs.SHA256Agg;
import edu.rice.historytree.generated.Serialization.PrunedTree;
import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;
import edu.rice.historytree.storage.HashStore;

public abstract class VerifyHisttreeCommon extends Verifier {
	public static HistoryTree<byte[],byte[]> parseHistoryTree(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();
		PrunedTree pb=sigblob.getTree();
		HistoryTree<byte[],byte[]> tree= new HistoryTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree.updateTime(pb.getVersion());
		tree.parseTree(pb);
		return tree;
	}

	public VerifyHisttreeCommon(SignaturePrimitives signer) {
		super(signer);
	}

	public boolean verifyHistoryRoot(Message message, HistoryTree<byte[],byte[]> parsed) {
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

}