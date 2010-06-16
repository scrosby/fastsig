package edu.rice.batchsig;

/** Represent a queue of messages to be signed with some sort of bulk signature mechanism. */
public interface ProcessQueue {

	/** Add a message to be processed */
	public abstract void add(Message message);

	/**
	 * Process all of the messages, signing every one. May be done in a separate
	 * signing thread
	 */
	abstract public void process();

}