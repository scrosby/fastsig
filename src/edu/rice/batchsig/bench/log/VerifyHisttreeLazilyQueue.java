package edu.rice.batchsig.bench.log;

import java.util.concurrent.ArrayBlockingQueue;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.bench.ShutdownableThread;
import edu.rice.batchsig.splice.VerifyHisttreeLazily;

/** Handle the glue code. 
 * 
 * Runs the thread that handles various incoming processing requests.
 * 
 * Handle the thread that handles the processing. */
public class VerifyHisttreeLazilyQueue extends ShutdownableThread implements ProcessQueue {
	VerifyHisttreeLazily treeverifier;

	final static int MAX_USERS = 10000;
	final static int MAX_MESSAGES = 50000;
	
	ArrayBlockingQueue<Integer> forcedUserMailbox = new ArrayBlockingQueue<Integer>(MAX_USERS);
	ArrayBlockingQueue<Message> messageMailbox = new ArrayBlockingQueue<Message>(MAX_MESSAGES);
	
	// Called concurrently.
	public void add(Message message) {
		if (message == null) 
			throw new Error("Cannot store null message");
		try {
			messageMailbox.put(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Called concurrently. 
	public void process() {
		// Nothing to do at the end of each batch. We're processing asynchronously at all times.
	}

	// called concurrently.
	public void finish() {
		// We're not really a queue, so this is never actually invoked.
		// Placeholder due to implementing ProcessQueue interface.
		throw new Error("Shouldn't be called");
	}

	// Called concurrently
	public int peekSize() {
		// The tree verifier uses an atomic int, allowing us to concurrently access it.
		return treeverifier.peekSize();
	}

	// Called concurrently
	public void forceUser(Integer i) {
		try {
			forcedUserMailbox.put(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	
	/** The core processing thread. */
	@Override
	public void run() {
		
		
	}
	
}
