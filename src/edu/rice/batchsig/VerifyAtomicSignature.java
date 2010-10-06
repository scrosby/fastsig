package edu.rice.batchsig;

import com.google.protobuf.ByteString;

import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;

public class VerifyAtomicSignature extends Verifier {

	public VerifyAtomicSignature(SignaturePrimitives signer) {
		super(signer);
	}

	public void add(Message message) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// Parse the tree.
		final byte[] rootHash = message.getData();
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MESSAGE)
			.setRoothash(ByteString.copyFrom(SimpleQueue.hash(rootHash)));

		message.signatureValidity(checkSig(sigblob, msgbuilder));
	}
	public void finishBatch() {
	}
}
