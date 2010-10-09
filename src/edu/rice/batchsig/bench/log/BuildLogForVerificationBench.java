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

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.MessageBase;
import edu.rice.batchsig.bench.OutgoingMessage;

/** Build the log based on a trace for events targetting a given destination. Used in the signature verification benchmark. */
public class BuildLogForVerificationBench {
	Object destinationTarget;
	int epochlength;
	
	public BuildLogForVerificationBench(int epochlength, Object target) {
		this.destinationTarget=target; // Doublecheck that all events are targetted to the SAME destination.
		this.epochlength = epochlength;
	}
	
	/* Build a file for the verification benchmark. IE, include real-time signatures indicating when stuff is signed. */
	public void makeTrace(Iterator<MessageEvent> events, Iterator<LogonLogoffEvent> logintimes, QueueBase queue) throws IOException {
		Iterator<LogonLogoffEvent> ii = logintimes;
		Iterator<MessageEvent> jj = events;
		LogonLogoffEvent i = ii.hasNext() ? ii.next() :null;
		MessageEvent e = jj.hasNext() ? jj.next() :null;

		double epochend = e.getTimestamp() + epochlength ;
		
		while (i != null || e != null) {
			ArrayList<Integer> logins = new ArrayList<Integer>();
			ArrayList<Integer> logouts = new ArrayList<Integer>();

			long eTime = e!= null ? e.getTimestamp() : Long.MAX_VALUE;
			long iTime = i!= null ? i.getTimestamp() : Long.MAX_VALUE;

			// Catch misuse.
			if (e != null && !e.getRecipientHost().equals(this.destinationTarget))
				throw new Error("Code only designed for one destination "+e.getRecipientHost() + " != "+ destinationTarget);

			// Time to run the queue?
			if (eTime > epochend && iTime > epochend) {
				epochend = e.getTimestamp() + epochlength;
				queue.process();
			}

			boolean addingMessage = false;
			if (e != null && i != null && i.getTimestamp() == e.getTimestamp()) {
				// We're putting both one signature and at least one stop/nonstop  into the queue 
				addingMessage = true;
			}
			
			
			double oldTimestamp = iTime;
			// If there's no event, then i!= null (otherwise we would have bailed earlier)
			if (e == null || i.getTimestamp() <= e.getTimestamp()) {
				// Add on all logons/logoffs that have the same timestamp.
				while (i != null && i.getTimestamp() == oldTimestamp) {
					if (i.getState() == LogonLogoffEvent.State.LOGON)
						logins.add(i.getRecipientUser());
					else
						logouts.add(i.getRecipientUser());
					i = ii.hasNext() ? ii.next() :null;
				}
				// Two cases:
				if (addingMessage) {
					// If we're merging with a message, fall through. 
				} else {
					// If we're NOT merging with a message, write it out now and continue the loop.
					writeLoginLogoutMsg(logins, logouts);
					continue;
				}
			}

			// Take the first message off of the queue.
			OutgoingMessage out = e.asOutgoingMessage(outstream);
			if (logins.size() > 0 || logouts.size() > 0) {
				out.setLoginsLogouts(logins,logouts);
			}
			queue.add(out);
			e = jj.hasNext() ? jj.next() :null;
			// Write out the last login/logout messages, if any.
			if (e == null && i == null)
				writeLoginLogoutMsg(logins, logouts);
		}
	}

	private void writeLoginLogoutMsg(ArrayList<Integer> logins,
			ArrayList<Integer> logouts) throws IOException {
		if (logins.size() > 0 || logouts.size() > 0) {
			OutgoingMessage out = new OutgoingMessage(outstream, null, destinationTarget,null);
			out.setLoginsLogouts(logins,logouts);
			out.writeTo(outstream);
		}
	}


	
	

	CodedOutputStream outstream;
	OutgoingMessage outmsg;

	void replaySign(EventTrace l, HashMap<Object,QueueBase> queuemap, HashMap<Object,CodedOutputStream> streammap) {
		Iterator<MessageEvent> i = l.iterator();
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
}
