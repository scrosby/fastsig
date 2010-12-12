package edu.rice.batchsig.lazy;

import edu.rice.batchsig.IMessage;

public interface VerifyLazily {

	// Called concurrently. Must be atomic int.
	int peekSize();

	void forceOldest();

	void add(IMessage m);

	void forceUser(Object i, long tstamp);

	void forceAll();
}
