package edu.rice.batchsig.bench.log;

import java.util.HashMap;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;
import edu.rice.batchsig.bench.OutgoingMessage;

public class LogReplaySigningBenchmarkThread extends MessageGeneratorThreadBase {
	Object sourceTarget;
	int epochlength;
	EventLog log;
	QueueBase senderqueue;
	HashMap<Object,CodedOutputStream> streammap;
	public LogReplaySigningBenchmarkThread(QueueBase queue, int maxsize, int epochlength, Object source) {
		super(queue,maxsize);
		this.sourceTarget=source; // Doublecheck that all events are sourced from the same source.
		this.epochlength = epochlength;
	}
	
	
	/** Replay signers from a single source to many recipients -- USED TO BENCHMARK SIGNING. THIS CODE ONLY INJECTS INTO THE QUEUE */
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.

		long bias = log.get(0).getTimestamp(); // Difference betweeen 'real' clock and virtual clock.
		for (Event e : log) {
			if (e != null && !e.getRecipient().equals(this.sourceTarget))
				throw new Error("Code only designed for one destination "+e.getSender() + " != "+ sourceTarget);

			
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipient()));
			
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
