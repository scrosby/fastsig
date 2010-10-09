package edu.rice.batchsig.bench.log;

import java.util.HashMap;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;
import edu.rice.batchsig.bench.OutgoingMessage;


/** Build a trace of messages from a signer to several recipients from a trace. Model 'real world' timing. */
public class ReplayAndQueueMessagesForSigningThread extends MessageGeneratorThreadBase {
	Object sourceTarget;
	EventTrace trace;
	HashMap<Object,CodedOutputStream> streammap;
	public ReplayAndQueueMessagesForSigningThread(QueueBase queue, int maxsize) {
		super(queue,maxsize);
	}
	
	public ReplayAndQueueMessagesForSigningThread configure(Object source, EventTrace log) {
		sourceTarget=source;
		this.trace = log;
		return this;
	}

	public ReplayAndQueueMessagesForSigningThread configure(HashMap<Object,CodedOutputStream> streammap) {
		this.streammap = streammap;
		return this;
	}

	
	/** Replay signers from a single source to many recipients -- USED TO BENCHMARK SIGNING. THIS CODE ONLY INJECTS INTO THE QUEUE */
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.

		long bias = trace.get(0).getTimestamp(); // Difference between 'real' clock and virtual clock.
		for (MessageEvent e : trace) {
			if (e != null && !e.getSenderHost().equals(this.sourceTarget))
				throw new Error("Code only designed for one destination "+e.getSenderHost() + " != "+ this.sourceTarget);
			
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipientHost()));
			
			// Offset in ms from the first message in the trace.
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
			System.out.println("Injecting msg "+ e.getTimestamp()+ " at " + injectTime);

			queue.add(msg);
			checkQueueOverflow();
		}
	}
	
}
