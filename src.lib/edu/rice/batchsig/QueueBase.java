package edu.rice.batchsig;

abstract public class QueueBase<T> {
	final protected AsyncQueue<T> queue = new AsyncQueue<T>();

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
	
}