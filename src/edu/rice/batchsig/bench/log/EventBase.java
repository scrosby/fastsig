package edu.rice.batchsig.bench.log;

public class EventBase {
	protected long timestamp;

	public EventBase(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long d) {
		this.timestamp = d;
	}

}