package edu.rice.batchsig;

import java.util.concurrent.atomic.AtomicBoolean;

/** Represents a thread that can be requested to exit. */
public class ShutdownableThread extends Thread {
	/** Has this thread been marked to be shuttingdown? */
	private AtomicBoolean shuttingdown = new AtomicBoolean(false);

	public ShutdownableThread() {
		super();
	}

	/** Request that this thread finish its work and exit. */
	public void shutdown() {
		shuttingdown.set(true);
	}
	
	/** Has this thread been marked as done? */
	protected boolean isShuttingdown() {
		return shuttingdown.get();
	}
}
