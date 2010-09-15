package edu.rice.batchsig.bench.log;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class MessageEvent extends EventBase {
	Object sender_host, recipient_host; // Hosts or servers.
	Object sender_user, recipient_user; // Usernames on those hosts.
	int size;
	
	MessageEvent(Object sender, Object recipient, long timestamp, int size) {
		super(timestamp);
		this.sender_host = sender;
		this.recipient_host = recipient;
		this.size = size;
	}
	
	
	public Object getRecipientHost() {
		return recipient_host;
	}
	public Object getSenderHost() {
		return sender_host;
	}

	public OutgoingMessage asOutgoingMessage(CodedOutputStream target) {
		OutgoingMessage msg = new OutgoingMessage(target,new byte[size], getRecipientHost());
		return msg;
	}
}
