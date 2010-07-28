package edu.rice.batchsig.bench.log;

import edu.rice.batchsig.Message;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class Event {
	Object sender, recipient;
	long timestamp;
	int size;
	
	Event(Object sender, Object recipient, long timestamp, int size) {
		this.sender = sender;
		this.recipient = recipient;
		this.timestamp = timestamp;
		this.size = size;
	}
	
	
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long d) {
		this.timestamp = d;
	}

	public Object getRecipient() {
		return recipient;
	}
	public Object getSender() {
		return sender;
	}
}
