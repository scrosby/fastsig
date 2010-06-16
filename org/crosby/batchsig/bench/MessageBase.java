package org.crosby.batchsig.bench;

import org.rice.crosby.batchsig.Message;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

abstract public class MessageBase implements Message{

	protected TreeSigBlob sigblob;
	protected byte [] data;

	public MessageBase() {
		super();
	}

	@Override
	public byte[] getData() {
		return data;
	}

}