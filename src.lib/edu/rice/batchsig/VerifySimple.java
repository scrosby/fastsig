package edu.rice.batchsig;

import com.google.protobuf.ByteString;

import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;

/** Verify messages signed with a simple signature. */
public class VerifySimple extends Verifier {

	public VerifySimple(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	public void add(IMessage message) {
		TreeSigBlob sigblob = message.getSignatureBlob();

		// Parse the tree.
		final byte[] rootHash = message.getData();
		TreeSigMessage.Builder msgbuilder = TreeSigMessage.newBuilder()
			.setTreetype(SigTreeType.MESSAGE)
			.setRoothash(ByteString.copyFrom(SimpleQueue.hash(rootHash)));

		message.signatureValidity(checkSig(sigblob, msgbuilder));
	}
	
	@Override
	public void process() {
	}
}
