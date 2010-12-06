/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.batchsig.bench.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.OMessage;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.SuspendableProcessQueue;
import edu.rice.batchsig.bench.MessageBase;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.batchsig.bench.PublicKeyPrims;

/** Build the log based on a trace for events targetting a given destination. Used in the signature verification benchmark. 
 * 
 * Algorithm: We model a large number of sources. Each 'real' message is inserted in a batch along with a collection of 'junk' messages that are ignored and round-out the batch.
 * The number of junk messages is based on what I see in my signing experiments.
 * 
 * We run a time-loop where for each epoch. (2ms with DSA, 15ms with RSA), we place real messages into the variosu signing queues along with junk messages, then process the queues, and save the output.
 * For each incoming message, assign it a 'random' source server, one for every queue.
 * 
 * Logonlogoff times are for users on a particular destination. The logfile is for all messages to that destination.
 * 
 * */



public class BuildLogForVerificationBench {
	final Object destinationTarget = new Integer(0);
	final private int epochlength;
	final SuspendableProcessQueue<OMessage> queues[];
	final private int sender_server_count;
	final private Supplier<Integer> batchsizefn;
	final private CodedOutputStream outstream;

	@SuppressWarnings("unchecked")
	public BuildLogForVerificationBench(int epochlength, Function<String,SuspendableProcessQueue<OMessage>> queuefactory, 
			int sender_server_count, Supplier<Integer> batchsizefn, CodedOutputStream outstream) {
		this.epochlength = epochlength;
		this.sender_server_count = sender_server_count;
		this.batchsizefn = batchsizefn;
		this.outstream = outstream;
		
		queues = (SuspendableProcessQueue<OMessage>[])new SuspendableProcessQueue<?>[sender_server_count];
		for (int i= 0 ; i < sender_server_count ; i++) {
			queues[i] = queuefactory.apply("Signer"+i); // Must match that used in BenchSigner.handleVerifyTrace
		}
	}
	
	/* Build a file for the verification benchmark. IE, include real-time signatures indicating when stuff is signed. */
	public void makeTrace(Iterator<MessageEvent> events, Iterator<LogonLogoffEvent> logintimes) throws IOException {
		Iterator<LogonLogoffEvent> ii = logintimes;
		Iterator<MessageEvent> jj = events;
		LogonLogoffEvent i = ii.hasNext() ? ii.next() :null;
		MessageEvent e = jj.hasNext() ? jj.next() :null;

		int counter = 0;

		Set<ProcessQueue<OMessage>> toRun = new HashSet<ProcessQueue<OMessage>>();
		
		for (int epochstart = 0 ; i != null && e != null ; epochstart += epochlength) {
			long epochend = e.getTimestamp() + epochlength;
			
			if (++counter % 100 == 0)
				System.err.format("Epoch: %d-%d\n", epochstart,epochend);

			// Add any messages in this epoch.
			while (e != null && e.getTimestamp() < epochend) {
				// Catch misuse.
				if (e != null && !e.getRecipientHost().equals(this.destinationTarget))
					throw new Error("Code only designed for one destination "+e.getRecipientHost() + " != "+ destinationTarget);

				int host = ((Integer) e.getSenderUser())% sender_server_count;
				OutgoingMessage out = e.asOutgoingMessage(outstream);
				queues[host].add(out);
				toRun.add(queues[host]);
				e = jj.hasNext() ? jj.next() :null;
			}

			/// Now run the queues.
			for (ProcessQueue<OMessage> queue : toRun) {
				// Add in junk messages to reach the target.
				int targetsize = batchsizefn.get();
				for (int j = queue.peekSize() ; j < targetsize ; j++)
					queue.add(JunkMessage.singleton);
				queue.process();
			}
			toRun.clear();

			// Add on the logonlogoff messages. 
			if (i != null && i.getTimestamp() < epochend) {
				ArrayList<Integer> logins = new ArrayList<Integer>();
				ArrayList<Integer> logouts = new ArrayList<Integer>();
				while (i != null && i.getTimestamp() < epochend) {
					if (i.getState() == LogonLogoffEvent.State.LOGON)
						logins.add(i.getRecipientUser());
					else
						logouts.add(i.getRecipientUser());
					i = ii.hasNext() ? ii.next() :null;
				}
				writeLoginLogoutMsg(logins, logouts, epochend);
			}

		}
		System.err.format("Finished trace\n");
		outstream.flush();
	}

	private void writeLoginLogoutMsg(ArrayList<Integer> logins,
			ArrayList<Integer> logouts, long timestamp) throws IOException {
		if (logins.size() > 0 || logouts.size() > 0) {
			OutgoingMessage out = new OutgoingMessage(outstream, null, destinationTarget,null);
			out.setLoginsLogouts(logins,logouts);
			out.setVirtualClock(timestamp);
			out.writeTo(outstream);
		}
	}

	/*
	void replaySign(Iterator<MessageEvent> i, HashMap<Object,QueueBase> queuemap, HashMap<Object,CodedOutputStream> streammap) {
		MessageEvent e = i.next();
		double epochend = e.getTimestamp() + epochlength ;
		Set<QueueBase> needsProcessing = new HashSet<QueueBase>();
		while (e != null) {
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipientHost()));
			QueueBase senderqueue = queuemap.get(e.getSenderHost());
			if (e.getTimestamp() > epochend) {
				epochend = e.getTimestamp() + epochlength;
				for (QueueBase queue : needsProcessing)
					queue.process();
				needsProcessing.clear();
			}
			senderqueue.add(msg);
			needsProcessing.add(senderqueue);
			e = i.hasNext() ? i.next() : null;
		}
	}	
	*/
}
