package edu.rice.batchsig.bench.log;

import edu.rice.batchsig.Message;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class JunkMessage implements Message {
	static final byte[] NODATA = new byte[0];

	@Override
	public byte[] getData() {
		return NODATA;
	}

	@Override
	public void signatureResult(TreeSigBlob message) {
		// Ignore
	}

	@Override
	public void signatureValidity(boolean valid) {
		throw new Error("Never called");
	}

	@Override
	public TreeSigBlob getSignatureBlob() {
		throw new Error("Never called");
	}

	@Override
	public Object getRecipient() {
		return new Object();
	}

	@Override
	public Object getAuthor() {
		throw new Error("Never called");
	}

}
