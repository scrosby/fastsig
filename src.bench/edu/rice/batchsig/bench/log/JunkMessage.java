package edu.rice.batchsig.bench.log;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.OMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class JunkMessage implements OMessage {
	static final byte[] NODATA = new byte[0];
	static final JunkMessage singleton = new JunkMessage();
	
	@Override
	public byte[] getData() {
		return NODATA;
	}

	@Override
	public void signatureResult(TreeSigBlob message) {
		// Ignore
	}

	@Override
	public Object getRecipient() {
		return new Object();
	}
}
