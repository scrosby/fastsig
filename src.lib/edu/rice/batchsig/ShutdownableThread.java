package edu.rice.batchsig;

import java.util.concurrent.atomic.AtomicBoolean;

/** Represents a thread that can be requested to exit. */
public class ShutdownableThread extends Thread {
	protected AtomicBoolean finished = new AtomicBoolean(false);

	public ShutdownableThread() {
		super();
	}

	/** Request that this thread finish its work and exit. */
	public void shutdown() {
		finished.set(true);
	}
}
