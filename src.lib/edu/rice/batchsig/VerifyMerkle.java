package edu.rice.batchsig;

import com.google.protobuf.ByteString;

import edu.rice.historytree.MerkleTree;
import edu.rice.historytree.aggs.SHA256Agg;
import edu.rice.historytree.generated.Serialization.PrunedTree;
import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;
import edu.rice.historytree.storage.HashStore;

/** Verify Merkle tree signatures */
public class VerifyMerkle extends Verifier {
	static public MerkleTree<byte[],byte[]> parseMerkleTree(IMessage message) {
		TreeSigBlob sigblob = message.getSignatureBlob();
		PrunedTree pb=sigblob.getTree();
		MerkleTree<byte[],byte[]> tree= new MerkleTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree.updateTime(pb.getVersion());
		tree.parseTree(pb);
		return tree;
	}

	public VerifyMerkle(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	public void add(IMessage message) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// Parse the tree.
		MerkleTree<byte[], byte[]> parsed = parseMerkleTree(message);

		// See if the message is in the tree.
		if (!checkLeaf(message, parsed))
			message.signatureValidity(false);
		
		final byte[] rootHash = parsed.agg();
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MERKLE_TREE)
			.setVersion(parsed.version())
			.setRoothash(ByteString.copyFrom(rootHash));

		message.signatureValidity(checkSig(sigblob, msgbuilder));
	}
	
	@Override
	public void process() {
		// We verify them when we add them. 
	}
}
