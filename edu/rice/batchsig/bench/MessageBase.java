package edu.rice.batchsig.bench;


import edu.rice.batchsig.Message;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

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