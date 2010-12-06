package edu.rice.batchsig.bench;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.ShutdownableThread;

/* Badly named class that processes messages at a given maxsize and dies if messages are generated too fast */
public class MessageGeneratorThreadBase<T> extends ShutdownableThread {

	private final int maxsize;
	protected final ProcessQueue<T> queue;
	static long lastErr = 0;
	static long skip = 0;

	public MessageGeneratorThreadBase(ProcessQueue<T> verifyqueue, int maxsize) {
		this.setName("Generate Messages");
		//if (verifyqueue == null)
		//	throw new Error("Null queue");
		this.queue = verifyqueue;
		this.maxsize = maxsize;
	}

	protected void checkQueueOverflow() {
		if (queue.peekSize() > maxsize) {
			skip++;
			if (System.currentTimeMillis() > lastErr + 1000) {
				System.err.format("Queue overfull(contains %d messages)\n",queue.peekSize());
				// Abort the second time we get a bad message.
				if (skip > 2) {
					shutdown();
					Tracker.singleton.markAbort();
					System.out.println("***** Marking message generation thread for finishing *******");
				}
				skip=0; lastErr = System.currentTimeMillis();
			}
		}
	}

}