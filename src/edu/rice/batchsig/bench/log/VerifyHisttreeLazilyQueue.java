package edu.rice.batchsig.bench.log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.bench.IncomingMessage;
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
	/** The paramater to tune */
	final static int MAX_OUTSTANDING = 100000;
	
	ArrayBlockingQueue<Integer> forcedUserMailbox = new ArrayBlockingQueue<Integer>(MAX_USERS);
	ArrayBlockingQueue<Long> forcedUserTimestampMailbox = new ArrayBlockingQueue<Long>(MAX_USERS);
	ArrayBlockingQueue<Message> messageMailbox = new ArrayBlockingQueue<Message>(MAX_MESSAGES);
	ArrayBlockingQueue<Message> forcedMessageMailbox = new ArrayBlockingQueue<Message>(MAX_MESSAGES);

	Semaphore sleepSemaphore = new Semaphore(0);
	
	public VerifyHisttreeLazilyQueue(VerifyHisttreeLazily treeverifier) {
		this.treeverifier = treeverifier;
	}
	
	// Called concurrently.
	public void add(Message message) {
		//System.out.println("Adding message "+message+ " into lazy queue");
		try {
			messageMailbox.put(message);
			sleepSemaphore.release();
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
			forcedUserTimestampMailbox.put(System.currentTimeMillis());
			sleepSemaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Called concurrently. Immediately add on the requested message.
	public void addForced(Message m) {
		try {
			((IncomingMessage)m).resetCreationTimeToNow();
			forcedMessageMailbox.put(m);
			sleepSemaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	boolean maximally_lazy = true;
	
	/** The core processing thread. */
	@Override
	public void run() {
		long verbose=0;
		long lastExpiration = 0;
		try {
			while (!finished.get()) {
				// Since we're idle? See if there's any useful work we could do right now.
				if (sleepSemaphore.tryAcquire()) {
					// We have work waiting.... Release the semaphore and do it.
					sleepSemaphore.release();
				} else {
					if (verbose++%10 == 0)
						System.out.println("Queuesize: "+treeverifier.peekSize());
					// No work that must be done right now, can we eagerly do something?
					if (!maximally_lazy) {
						long now = System.currentTimeMillis();
						if (now-lastExpiration > 5000 || treeverifier.peekSize() > 1000) {
							if (now-lastExpiration > 5000)
								System.out.println("Forcing because of age");
							if (treeverifier.peekSize() > 1000)
								System.out.format("Forcing because of size %d > 1000\n",treeverifier.peekSize());
							treeverifier.forceOldest();
							lastExpiration = now; 
							continue; // And try again for more eager work.
						}
					} else {
						if (treeverifier.peekSize() > MAX_OUTSTANDING) {
							System.out.format("Forcing because of size %d > %d\n",treeverifier.peekSize(),MAX_OUTSTANDING);
							treeverifier.forceOldest();
							continue; // And try again for more eager work.
						}
					}
				}
				
				// There's something sitting around to be done right now.
				sleepSemaphore.acquire();

				// Is it a message to process?
				Message m;
				m = messageMailbox.poll();
				if (m != null) {
					treeverifier.add(m);
					continue;
				}
				// Is it a message to process right now?
				m = forcedMessageMailbox.poll();
				if (m != null) {
					throw new Error("TEST POS");
					//treeverifier.add(m);
					//treeverifier.force((IncomingMessage)m);
					//continue;
				}

				// Is it a user to force?
				Integer i = forcedUserMailbox.poll();
				if (i != null) {
					Long tstamp = forcedUserTimestampMailbox.poll();
					treeverifier.forceUser(i,tstamp);
					continue;
				}
				throw new Error("Should never get here");
			}
			System.out.println("End mailbox loop");
		} catch (InterruptedException e) {
		}

		
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
