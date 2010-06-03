package org.rice.crosby.batchsig;

import java.util.Arrays;

import org.rice.crosby.historytree.HistoryTree;
import org.rice.crosby.historytree.MerkleTree;
import org.rice.crosby.historytree.NodeCursor;
import org.rice.crosby.historytree.TreeBase;
import org.rice.crosby.historytree.aggs.SHA256Agg;
import org.rice.crosby.historytree.generated.Serialization.PrunedTree;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.TreeType;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.ByteString;

public class Verifier {
	private Signer signer;

	public Verifier(Signer signer) {
		this.signer=signer;
	}
	
	boolean verify(Message message) {
		TreeSigBlob sigblob = message.getSigBlob();
		
		// Other choices are unsupported in this code.
		if (sigblob.getTreetype() == TreeType.SINGLE_MERKLE_TREE)
			return verifyMerkle(message);
		else if (sigblob.getTreetype() == TreeType.SINGLE_MERKLE_TREE)
			return verifyHistory(message);
		else 
			return false;

	}

	boolean verifyMerkle(Message message) {
		TreeSigBlob sigblob = message.getSigBlob();

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
		TreeSigBlob sigblob = message.getSigBlob();

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
		TreeSigBlob sigblob = message.getSigBlob();

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

	private boolean checkSig(TreeSigBlob sigblob, TreeSigMessage.Builder msgbuilder) {
		byte[] signeddata = msgbuilder.build().toByteArray();
		byte[] sig = sigblob.getSig().toByteArray();

		return signer.verify(signeddata, sig);
	}

	static public MerkleTree<byte[],byte[]> parseMerkleTree(Message message) {
		TreeSigBlob sigblob = message.getSigBlob();
		PrunedTree pb=sigblob.getTree();
		MerkleTree<byte[],byte[]> tree= new MerkleTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree.updateTime(pb.getVersion());
		tree.parseTree(pb);
		return tree;
	}	
	static public HistoryTree<byte[],byte[]> parseHistoryTree(Message message) {
		TreeSigBlob sigblob = message.getSigBlob();
		PrunedTree pb=sigblob.getTree();
		HistoryTree<byte[],byte[]> tree= new HistoryTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree.updateTime(pb.getVersion());
		tree.parseTree(pb);
		return tree;
	}	
}
