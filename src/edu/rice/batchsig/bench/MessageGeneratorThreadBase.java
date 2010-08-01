package edu.rice.batchsig.bench;

import edu.rice.batchsig.QueueBase;

public class MessageGeneratorThreadBase extends ShutdownableThread {

	protected final int rate;
	protected final QueueBase queue;
	static long lastErr = 0;
	static long skip = 0;

	public MessageGeneratorThreadBase(QueueBase verifyqueue, int rate) {
		if (verifyqueue == null)
			throw new Error();
		this.queue = verifyqueue;
		this.rate = rate;
	}

	protected void checkQueueOverflow() {
		if (queue.peekSize() > rate) {
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