package edu.rice.batchsig.bench.log;

import java.util.HashMap;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;
import edu.rice.batchsig.bench.OutgoingMessage;

public class ReplayAndQueueMessagesForSigningThread extends MessageGeneratorThreadBase {
	Object sourceTarget;
	EventLog log;
	QueueBase senderqueue;
	HashMap<Object,CodedOutputStream> streammap;
	public ReplayAndQueueMessagesForSigningThread(QueueBase queue, int maxsize) {
		super(queue,maxsize);
	}
	
	public ReplayAndQueueMessagesForSigningThread configure(Object source, EventLog log) {
		sourceTarget=source;
		this.log = log;
		return this;
	}
	
	
	/** Replay signers from a single source to many recipients -- USED TO BENCHMARK SIGNING. THIS CODE ONLY INJECTS INTO THE QUEUE */
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.

		long bias = log.get(0).getTimestamp(); // Difference between 'real' clock and virtual clock.
		for (MessageEvent e : log) {
			if (e != null && !e.getRecipientHost().equals(this.sourceTarget))
				throw new Error("Code only designed for one destination "+e.getSenderHost() + " != "+ sourceTarget);
			
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipientHost()));
			
			// Offset in ms from the first message in the log.
			long msgOffsetTime = e.getTimestamp()-bias;
			// What time should we insert.
			long injectTime = msgOffsetTime + initTime;
			long now = System.currentTimeMillis();

			if (injectTime > now) {
				// Running ahead. Lets sleep for a little bit. 
				try {
					Thread.sleep(injectTime-now);
				} catch (InterruptedException exn) {
				}
			}

			senderqueue.add(msg);
			checkQueueOverflow();
		}
	}
	
}
