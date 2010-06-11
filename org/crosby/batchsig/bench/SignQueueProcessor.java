package org.crosby.batchsig.bench;

import java.util.concurrent.atomic.AtomicBoolean;

import org.rice.crosby.batchsig.QueueBase;

public class SignQueueProcessor implements Runnable {
	private int epochlength;
	private QueueBase signqueue;
	private AtomicBoolean finished = new AtomicBoolean(false);
	
	
	SignQueueProcessor(QueueBase signqueue, int epochtime) {
		this.signqueue = signqueue;
		this.epochlength = epochtime;
	}
	

	public void shutdown() {
		finished.set(true);
	}
	
	
	@Override
	public void run() {
		while (!finished.get()) {
			long epochstart = System.currentTimeMillis();
			signqueue.process();

			long now = System.currentTimeMillis();
			while (now < epochlength+epochstart) {
				try {
					Thread.sleep(epochlength+epochstart-now);
				} catch (InterruptedException e) {
				}
				now = System.currentTimeMillis();
			}
		}
		signqueue.process();
	}
}
