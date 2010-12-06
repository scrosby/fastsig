package edu.rice.batchsig;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownableThread extends Thread {
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