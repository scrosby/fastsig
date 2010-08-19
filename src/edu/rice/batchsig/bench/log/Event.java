package edu.rice.batchsig.bench.log;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class Event extends EventBase {
	Object sender, recipient; // Hosts or servers.
	Object sender_user, recipient_user; // Usernames on those hosts.
	int size;
	
	Event(Object sender, Object recipient, long timestamp, int size) {
		super(timestamp);
		this.sender = sender;
		this.recipient = recipient;
		this.size = size;
	}
	
	
	public Object getRecipient() {
		return recipient;
	}
	public Object getSender() {
		return sender;
	}

	public OutgoingMessage asOutgoingMessage(CodedOutputStream target) {
		OutgoingMessage msg = new OutgoingMessage(target,new byte[size], getRecipient());
		return msg;
	}
}
