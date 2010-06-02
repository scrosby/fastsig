package org.rice.crosby.batchsig;

/** Represent a queue of messages to be signed with some sort of bulk signature mechanism. */
public interface SignerQueue {

	public abstract void add(Message message);

	/**
	 * Process all of the messages, signing every one. May be done in a separate
	 * signing thread
	 */
	abstract public void process(Message message);

}