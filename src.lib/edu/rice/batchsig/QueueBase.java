package edu.rice.batchsig;

import java.util.ArrayList;

/**
 * Common code shared by the different signing queue implementations.
 * 
 * For the most part, wraps an underlying AsyncQueue for managing the
 * outstanding unprocessed messages.
 * */
abstract public class QueueBase<T> implements ProcessQueue<T> {
	final private AsyncQueue<T> queue = new AsyncQueue<T>();

	// Called Async. 
	public void add(T m) {
		queue.add(m);
	}

	public void finish() {
		process();
	}
	
	public int peekSize() {
		return queue.peekSize();
	}
	
	public abstract void process();

	
	/** 
	 * @see edu.rice.batchsig.AsyncQueue#atomicGetQueue
	 */
	protected ArrayList<T> atomicGetQueue() {
		return queue.atomicGetQueue();
	}
	
	/** 
	 * @see edu.rice.batchsig.AsyncQueue#suspendTillNonEmpty
	 */
	public void suspendTillNonEmpty() {
		queue.suspendTillNonEmpty();
	}
}