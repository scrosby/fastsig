package edu.rice.batchsig.bench;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownableThread extends Thread {
	protected AtomicBoolean finished = new AtomicBoolean(false);

	public ShutdownableThread() {
		super();
	}

	public void shutdown() {
		finished.set(true);
		this.interrupt();
	}

}