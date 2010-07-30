package edu.rice.batchsig.bench;

public interface ShutdownableThread {

	public abstract void shutdown();

	public abstract void start();

	public abstract void join() throws InterruptedException;

}