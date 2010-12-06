package edu.rice.batchsig;

import java.util.ArrayList;

abstract public class QueueBase<T> {
	final private AsyncQueue<T> queue = new AsyncQueue<T>();

	public QueueBase() {
		super();
	}
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

	public AsyncQueue<T> getAsync() {
		return queue;
	}
	protected ArrayList<T> atomicGetQueue() {
		return queue.atomicGetQueue();
	}
}