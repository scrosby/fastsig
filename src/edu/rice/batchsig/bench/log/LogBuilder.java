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

/** Build the log that gets targetted to a given destination */
public class LogBuilder {
	List<LogonLogoffEvent> logintimes;
	EventLog events;
	Object destinationTarget;
	
	
	public void doBench(QueueBase queue) throws IOException {
		ArrayList<Integer> logins = new ArrayList<Integer>();
		ArrayList<Integer> logouts = new ArrayList<Integer>();

		
		Iterator<LogonLogoffEvent> ii = logintimes.iterator();
		Iterator<Event> jj = events.iterator();
		LogonLogoffEvent i = ii.hasNext() ? ii.next() :null;
		Event e = jj.hasNext() ? jj.next() :null;

		double epochend = e.getTimestamp() + EPOCHLENGTH ;
		
		while (i != null || e != null) {
			// Catch misuse.
			if (e != null && !e.getRecipient().equals(this.destinationTarget))
				throw new Error("Code only designed for one destination "+i + " != "+ destinationTarget);

			// Time to run the queue?
			if (e.getTimestamp() > epochend) {
				epochend = e.getTimestamp() + EPOCHLENGTH;
				queue.process();
			}

			
			double oldTimestamp = i.getTimestamp();
			if (e == null || i.getTimestamp() <= e.getTimestamp()) {
				if (i.getState() == LogonLogoffEvent.State.LOGON)
					logins.add(i.getUser());
				else
					logouts.add(i.getUser());
				i = ii.hasNext() ? ii.next() :null;
				
				// Three casees:
				if (i != null && i.getTimestamp() == oldTimestamp)
					continue; // Accumulate, lots of logons/logoffs at the same time.
				// Two cases. Either the event has the same timestamp and merges, or...
				if (e != null && i.getTimestamp() > e.getTimestamp()) {
					writeLoginLogoutMsg(logins, logouts);
					logins = new ArrayList<Integer>();
					logouts = new ArrayList<Integer>();
				}
			}

			// Store the next message.
			if (i == null || oldTimestamp >= e.getTimestamp()) {
				OutgoingMessage out = e.asOutgoingMessage(outstream);
				if (logins.size() > 0 || logouts.size() > 0) {
					out.setLoginsLogouts(logins,logouts);
				}
				queue.add(out);
				e = jj.hasNext() ? jj.next() :null;
			}
		}
		writeLoginLogoutMsg(logins, logouts);
		
	}

	private void writeLoginLogoutMsg(ArrayList<Integer> logins,
			ArrayList<Integer> logouts) throws IOException {
		if (logins.size() > 0 || logouts.size() > 0) {
			OutgoingMessage out = new OutgoingMessage(outstream, null, destinationTarget);
			out.setLoginsLogouts(logins,logouts);
			out.writeTo(outstream);
		}
	}


	
	
	final int EPOCHLENGTH = 1000;

	CodedOutputStream outstream;
	OutgoingMessage outmsg;

	void replaySign(EventLog l, HashMap<Object,QueueBase> queuemap, HashMap<Object,CodedOutputStream> streammap) {
		Iterator<Event> i = l.iterator();
		Event e = i.next();
		double epochend = e.getTimestamp() + EPOCHLENGTH ;
		Set<QueueBase> needsProcessing = new HashSet<QueueBase>();
		while (e != null) {
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipient()));
			QueueBase senderqueue = queuemap.get(e.getSender());
			if (e.getTimestamp() > epochend) {
				epochend = e.getTimestamp() + EPOCHLENGTH;
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
