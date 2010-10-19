package edu.rice.batchsig.bench.log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
	
	public VerifyHisttreeLazilyQueue(VerifyHisttreeLazily treeverifier) {
		this.treeverifier = treeverifier;
	}
	
	// Called concurrently.
	public void add(Message message) {
		//System.out.println("Adding message "+message+ " into lazy queue");
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
		System.out.println("Ordering processing to finish");
		finished.set(true);
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

	boolean maximally_lazy = false;
	
	/** The core processing thread. */
	@Override
	public void run() {
	
		long lastExpiration = 0;
		while (!finished.get()) {
			while (true) {
				Message m = messageMailbox.poll();
				if (m == null)
					break;
				treeverifier.add(m);
			}
			// If we're not being lazy, force the oldest message.
			if (!maximally_lazy) {
				while (true) {
					Integer i = forcedUserMailbox.poll();
					if (i == null)
						break;
					treeverifier.forceUser(i);
				}
				long now = System.currentTimeMillis();
				if (now-lastExpiration > 5000 || treeverifier.peekSize() > 1000) {
					if (now-lastExpiration > 5000)
						System.out.println("Forcing because of age");
					if (treeverifier.peekSize() > 1000)
						System.out.format("Forcing because of size %d > 1000",treeverifier.peekSize());
					treeverifier.forceOldest();
					lastExpiration = now;
				}
			} else {
				while (true) {
					Integer i;
					try {
						i = forcedUserMailbox.take();
					} catch (InterruptedException e) {
						break;
					}
					if (i == null)
						break;
					System.out.println("Forcing user "+i);
					treeverifier.forceUser(i);
				}
			}
			}
		System.out.println("End mailbox loop");

		// Add any remaining messages in mailbox.
		while (true) {
			Message m = messageMailbox.poll();
			if (m == null)
				break;
			treeverifier.add(m);
		}
		
		treeverifier.forceAll();
	}
	
}
