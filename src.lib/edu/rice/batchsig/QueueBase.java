package edu.rice.batchsig;

import java.util.ArrayList;

/**
 * Common code shared by the different signing queue and verifying queue implementations.
 * 
 * For the most part, wraps an underlying AsyncQueue for managing the
 * outstanding unprocessed messages.
 * */
abstract public class QueueBase<T> implements SuspendableProcessQueue<T> {
	/** The underlying async queue that manages outstanding messages */
	private final AsyncQueue<T> queue = new AsyncQueue<T>();

	/** Underlying public key signature algorithm. */
	protected final SignaturePrimitives signer;

	/** Construct a basic queue from the signer. */
	QueueBase(SignaturePrimitives signer) {
		if (signer == null)
			throw new NullPointerException();
		this.signer = signer;
	}
	
	
	@Override
	public void add(T m) {
		queue.add(m);
	}

	@Override
	public void finish() {
		process();
	}
	
	@Override
	public int peekSize() {
		return queue.peekSize();
	}
	
	@Override
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
	@Override
	public void suspendTillNonEmpty() {
		queue.suspendTillNonEmpty();
	}
}