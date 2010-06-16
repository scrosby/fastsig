package edu.rice.batchsig;

import java.util.ArrayList;

public abstract class QueueBase implements ProcessQueue {
	private ArrayList<Message> queue;

	public QueueBase() {
		super();
		initQueue();
	}

	/* (non-Javadoc)
	 * @see edu.rice.batchsig.SignerQueue#add(edu.rice.batchsig.Message)
	 */
	public synchronized void add(Message message) {
		queue.add(message);
		this.notify();
	}

	/** Get the current queue size */
	public synchronized int peekSize() {
		return queue.size();
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
	 * @see edu.rice.batchsig.SignerQueue#process(edu.rice.batchsig.Message)
	 */
	abstract public void process();
}
