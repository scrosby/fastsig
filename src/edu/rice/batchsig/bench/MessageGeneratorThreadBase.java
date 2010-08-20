package edu.rice.batchsig.bench;

import edu.rice.batchsig.QueueBase;

/* Badly named class that processes messages at a given maxsize and dies if messages are generated too fast */
public class MessageGeneratorThreadBase extends ShutdownableThread {

	protected final int maxsize;
	protected final QueueBase queue;
	static long lastErr = 0;
	static long skip = 0;

	public MessageGeneratorThreadBase(QueueBase verifyqueue, int maxsize) {
		if (verifyqueue == null)
			throw new Error();
		this.queue = verifyqueue;
		this.maxsize = maxsize;
	}

	protected void checkQueueOverflow() {
		if (queue.peekSize() > maxsize) {
			skip++;
			if (System.currentTimeMillis() > lastErr + 1000) {
				System.err.format("Queue overfull(%d)\n",skip);
				if (skip > 1)
					Tracker.singleton.markAbort();
				skip=0; lastErr = System.currentTimeMillis();
			}
		}
	}

}