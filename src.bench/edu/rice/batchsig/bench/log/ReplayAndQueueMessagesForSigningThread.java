package edu.rice.batchsig.bench.log;

import java.util.HashMap;
import java.util.Iterator;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.OMessage;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;
import edu.rice.batchsig.bench.OutgoingMessage;


/** Build a trace of messages from a signer to several recipients from a trace. Model 'real world' timing. */
public class ReplayAndQueueMessagesForSigningThread extends MessageGeneratorThreadBase<OMessage> {
	Object sourceTarget;
	Iterator<MessageEvent> trace;
	HashMap<Object,CodedOutputStream> streammap;
	public ReplayAndQueueMessagesForSigningThread(ProcessQueue<OMessage> queue, int maxsize) {
		super(queue,maxsize);
	}
	
	public ReplayAndQueueMessagesForSigningThread configure(Object source, Iterator<MessageEvent> events) {
		sourceTarget=source;
		this.trace = events;
		return this;
	}

	public ReplayAndQueueMessagesForSigningThread configure(HashMap<Object,CodedOutputStream> streammap) {
		this.streammap = streammap;
		return this;
	}

	
	/** Replay signers from a single source to many recipients -- USED TO BENCHMARK SIGNING. THIS CODE ONLY INJECTS INTO THE QUEUE */
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.

		long bias = 0, lastEventTime =0;
		int i = 0;
		while (queue.peekSize() < 6000 && trace.hasNext()) {
			i++;
			MessageEvent e = trace.next();
			if (bias == 0)
				lastEventTime = bias = e.getTimestamp(); // Difference between 'real' clock and virtual clock.
	        
			if (e != null && !e.getSenderHost().equals(this.sourceTarget))
				throw new Error("Code only designed for one destination "+e.getSenderHost() + " != "+ this.sourceTarget);

			if (e.getTimestamp() < lastEventTime)
				throw new Error("Log file should be sorted");
			lastEventTime = e.getTimestamp();
			
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
			if (i%1000 == 0 || injectTime-now > 2000)
				System.err.println("Injecting msg #"+ i + " late by "+(injectTime-now));

			queue.add(msg);
			checkQueueOverflow();
		}
		System.err.println("#######Stopping message injection");
	}
	
}
