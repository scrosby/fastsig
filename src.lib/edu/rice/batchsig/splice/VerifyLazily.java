package edu.rice.batchsig.splice;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.bench.IncomingMessage;

public interface VerifyLazily {

	// Called concurrently. Must be atomic int.
	int peekSize();

	void forceOldest();

	void add(Message m);

	void force(IncomingMessage m);

	void forceUser(Object i, long tstamp);

	void forceAll();

	void messageValidatorCallback(IncomingMessage incomingMessage);

}
