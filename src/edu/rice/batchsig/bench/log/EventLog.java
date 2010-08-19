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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.MessageBase;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.batchsig.bench.PublicKeyPrims;

/** Contain a queue of timestamped messages queued by arrival time. */
public class EventLog implements Iterable<Event> {
	Event log[];


	/** Add a timestamp offset to all messages in the log. */
	public void offset(long offset) {
		for (EventBase i : log) {
			i.setTimestamp(i.getTimestamp()+offset);
		}		
	}
	
	/* TODO:Filter everything in the log to only messages in a particular time range */
	
	
	/** Merges two logs together, permanently mutating BOTH logs */
	public void merge(EventLog peer) {
		// We're merging in something empty.
		if (peer.log.length == 0)
			return;
		
		// We're empty, just copy the log array.
		if (this.log.length == 0) {
			this.log = peer.log;
			peer.log = null; // Catch bugs.
			return;
		}
				
		Iterator<Event> j = this.iterator();
		Iterator<Event> k = peer.iterator();

		Event jm = j.next();
		Event km = k.next();

		int i=0;
		Event out[] = new Event[this.log.length+peer.log.length];
		
		// Assumes that the iterator returns null if we iterate past the end.
		while (jm != null || km != null) {
			// If jm is null then km can't be.
			if (jm == null) {
				out[i++] = km; km = k.next(); continue;
			}
			// Vice versa. 
			if (km == null) {
				out[i++] = jm; jm = j.next(); continue;
			}

			// Both are not null. Easy!
			if (jm.getTimestamp() < km.getTimestamp()) {
				out[i++] = jm;
				jm = j.next();
				continue;
			}
			if (km.getTimestamp() < jm.getTimestamp()) {
				out[i++] = km;
				km = k.next();
				continue;
			}
		}
		this.log = out;
		peer.log = null; // Catch bugs.
	}

	/* Iterator returns null if we iterate past the end */
	public Iterator<Event> iterator() {
		return new Iter();
	}

	class Iter implements Iterator<Event> {
		int index = 0;
		@Override
		public boolean hasNext() {
			return index < log.length;
		}

		@Override
		public Event next() {
			if (index < log.length)
				return log[index++];
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/** Build a set of all of the recipient objects */
	public Set<Object> buildRecipientSet() {
		Set<Object> out = new HashSet<Object>();
		for (Event e : this) {
			out.add(e.getRecipient());
		}
		return out;
	}

	/** Build a set of all of the recipient senders */
	public Set<Object> buildSenderSet() {
		Set<Object> out = new HashSet<Object>();
		for (Event e : this) {
			out.add(e.getSender());
		}
		return out;
	}

	public void keepOnlySender(Object sender) {
		ArrayList<Event> tmp = new ArrayList<Event>();
		for (Event e : this)
			if (sender.equals(e.getSender()))
				tmp.add(e);
		log = tmp.toArray(new Event[0]);
	}
	
	public void keepOnlyRecipient(Object recipient) {
		ArrayList<Event> tmp = new ArrayList<Event>();
		for (Event e : this)
			if (recipient.equals(e.getRecipient()))
				tmp.add(e);
		log = tmp.toArray(new Event[0]);
	}

	final double EPOCHLENGTH = .100;
	
	/** Replay signers from a single source to many recipients -- USED TO BENCHMARK SIGNING. */
	void replaySign(QueueBase senderqueue, HashMap<Object,CodedOutputStream> streammap) {
		double epochend = log[0].getTimestamp() + EPOCHLENGTH ;
		for (Event e : log) {
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipient()));
			if (e.getTimestamp() > epochend) {
				epochend = e.getTimestamp() + EPOCHLENGTH;
				senderqueue.process();
			}
			senderqueue.add(msg);
		}
	}
	
	/** Replay signers from from many sources to many recipients. 
	 * 
	 * UNUSED: DOES NOT SUPPORT LOGON/LOGOFF INFO. Can be used to generate a replay file for verification.  */
	void replaySign(HashMap<Object,QueueBase> queuemap, HashMap<Object,CodedOutputStream> streammap) {
		double epochend = log[0].getTimestamp() + EPOCHLENGTH ;
		Set<QueueBase> needsProcessing = new HashSet<QueueBase>();
		for (Event e : log) {
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
		}
	}

	MultiplexedPublicKeyPrims prims;
		
	/* Simulation infrastructure:
	 * 
	 *  When signing: 
	 *     We have one queue for each sender object; we model each sender independently and use a virtual clock, triggering process()'s when appropriate. 
	 *     Each destination is placed into a different output file. 
     *         This is done by instantiating OutgoingMessage with the correct outgoing stream.
	 *
     *     Each distinct signer has a separate queue for processing, and its own publickeyprims.
	 *
	 *  When verifying:
	 *
	 *     Given a message log (containing messages to only one recipient). play it, verifying all signatures.
	 *     Process the queue per the virtual clock.
	 *     
	 *     Create 'PublicKeyPrims' objects containing the signer's public keys on-the-fly, using the information in the sender_id's in the message bodies. 
	 *     Record various statistics.
	 *  
	 *  */




}