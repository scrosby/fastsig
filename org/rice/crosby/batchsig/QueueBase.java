package org.rice.crosby.batchsig;

import java.util.ArrayList;

public class QueueBase {

	protected ArrayList<Message> queue;

	public QueueBase() {
		super();
	}

	public void add(Message message) {
		synchronized(this) {
			queue.add(message);
		}
	}

	protected void initQueue() {
		queue = new ArrayList<Message>(32);
	}

	/**
	 * Get the queued messages which need to be signed atomically. This
	 * permanently removes them from the queue and the invoker is responsible
	 * for processing (or re-queuing) the messages.
	 */
	protected ArrayList<Message> atomicGetQueue() {
		ArrayList<Message> oldqueue;
		synchronized (this) {
			oldqueue = queue;
			initQueue();
		}
		return oldqueue;
	}

}