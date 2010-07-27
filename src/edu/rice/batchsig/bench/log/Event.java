package edu.rice.batchsig.bench.log;

import edu.rice.batchsig.Message;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class Event {
	Object sender, recipient;
	double timestamp;
	int size;
	
	Event(Object sender, Object recipient, double timestamp, int size) {
		this.sender = sender;
		this.recipient = recipient;
		this.timestamp = timestamp;
		this.size = size;
	}
	
	
	public double getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(double d) {
		this.timestamp = d;
	}

	public Object getRecipient() {
		return recipient;
	}
	public Object getSender() {
		return sender;
	}
}
