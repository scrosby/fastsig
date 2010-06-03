package org.rice.crosby.batchsig;

import java.util.ArrayList;

public abstract class QueueBase implements ProcessQueue {
	private ArrayList<Message> queue;

	public QueueBase() {
		super();
		initQueue();
	}

	/* (non-Javadoc)
	 * @see org.rice.crosby.batchsig.SignerQueue#add(org.rice.crosby.batchsig.Message)
	 */
	public synchronized void add(Message message) {
		queue.add(message);
		this.notify();
	}

	private void initQueue() {
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

	/** Suspend the calling thread until the queue is non-empty */
	public synchronized void suspendTillNonEmpty() {
		try {
			this.wait();
		} catch (InterruptedException e) {
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.rice.crosby.batchsig.SignerQueue#process(org.rice.crosby.batchsig.Message)
	 */
	abstract public void process();
}
