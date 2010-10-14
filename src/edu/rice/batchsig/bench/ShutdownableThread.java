package edu.rice.batchsig.bench;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownableThread extends Thread {
	// How long to wait before polls with the sleep?
	final static int SLEEP_POLL = 1000;
	
	protected AtomicBoolean finished = new AtomicBoolean(false);

	public ShutdownableThread() {
		super();
	}

	public void shutdown() {
		//System.out.println("Markign thread for shutdown");
		finished.set(true);
		//this.interrupt();
		//throw new Error("FAIL");
	}
}