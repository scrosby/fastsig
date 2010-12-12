package edu.rice.batchsig.bench.log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.rice.batchsig.AsyncQueue;
import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.Message;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.ShutdownableThread;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.lazy.VerifyHisttreeLazily;
import edu.rice.batchsig.lazy.VerifyLazily;

/** Handle the glue code. 
 * 
 * Runs the thread that handles various incoming processing requests.
 * 
 * Handle the thread that handles the processing. */
public class VerifyHisttreeLazilyQueue extends ShutdownableThread implements ProcessQueue<IMessage> {
	VerifyLazily treeverifier;

	final static int MAX_USERS = 10000;
	final static int MAX_MESSAGES = 50000;
	/** The paramater to tune */
	final static int MAX_OUTSTANDING = 40000;
	
	ArrayBlockingQueue<Integer> forcedUserMailbox = new ArrayBlockingQueue<Integer>(MAX_USERS);
	ArrayBlockingQueue<Long> forcedUserTimestampMailbox = new ArrayBlockingQueue<Long>(MAX_USERS);
	ArrayBlockingQueue<IMessage> messageMailbox = new ArrayBlockingQueue<IMessage>(MAX_MESSAGES);

	
	// Release the semaphore each time we add some form of work to be processed (IE, a new message or user being forced.
    // It is empty if there is nothing to do. 
	Semaphore sleepSemaphore = new Semaphore(0);
	
	public VerifyHisttreeLazilyQueue(VerifyLazily treeverifier) {
		this.treeverifier = treeverifier;
	}
	
	// Called concurrently.
	public void add(IMessage message) {
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
		// Order the underlying thread to quit.
		super.shutdown();
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

	boolean maximally_lazy = false;
	
	/** The core processing thread. */
	@Override
	public void run() {
		long verbose=0;
		long lastExpiration = 0;
		long lastMessage=0;
		try {
			while (!isShuttingdown()) {
				// Since we're idle? See if there's any useful work we could do right now.
				if (sleepSemaphore.tryAcquire()) {
					// We have work waiting.... Release the semaphore and do it.
					sleepSemaphore.release();
				} else {
					// No work waiting to do. See if we can eagerly use this idle time.
					if (verbose++%100 == 0)
						System.out.println("Queuesize: "+treeverifier.peekSize());
					// No work that must be done right now, can we eagerly do something?
					if (!maximally_lazy) {
						//long now = System.currentTimeMillis();
						if (treeverifier.peekSize() > 100) {
							treeverifier.forceOldest();
							continue; // And try again for more eager work.
						}
					} else {
						if (treeverifier.peekSize() > MAX_OUTSTANDING) {
							System.out.format("Forcing because of size %d > %d\n",treeverifier.peekSize(),MAX_OUTSTANDING);
							treeverifier.forceOldest();
							continue; // And try again for more eager work.
						}
					}
					// Only way we got here is if there is no work we could do. Time to block (on the acquire below)
				}
				
				// There's something sitting around to be done right now.
				sleepSemaphore.acquire();
				IMessage m;

				// Is it a message to process?
				m = messageMailbox.poll();
				if (m != null) {
					if (verbose++%5000 == 0)
						System.out.println("Virt: "+((IncomingMessage)m).getVirtualClock());
					treeverifier.add(m);
					continue;
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
			IMessage m = messageMailbox.poll();
			if (m == null)
				break;
			treeverifier.add(m);
		}
		
		treeverifier.forceAll();
	}


}
