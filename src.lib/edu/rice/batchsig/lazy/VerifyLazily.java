package edu.rice.batchsig.lazy;

import edu.rice.batchsig.IMessage;

public interface VerifyLazily {

	/**
	 * Get the size of the queue. May be called concurrently from any number of
	 * threads. Called concurrently, must be an AtomicInt.
	 */
	int peekSize();

	/** Force the oldest message in this queue. */
	void forceOldest();

	/** Add a message to be lazily verified. */
	void add(IMessage m);

	/** Force all messages by the one user. */
	void forceUser(Object i, long tstamp);

	/** Force all messages in the queue. */
	void forceAll();
}
