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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.MessageBase;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.batchsig.bench.PublicKeyPrims;

/** Contain a queue of timestamped messages queued by arrival time. */
public class EventTrace implements Iterable<MessageEvent> {
	MessageEvent log[];

	EventTrace(List<MessageEvent> list) {
		log = list.toArray(new MessageEvent[0]);
		System.err.println("Sorting trace");
		EventBase.sort(log);
	}

	public EventTrace(Iterator<MessageEvent> it) {
		ArrayList<MessageEvent> tmp = new ArrayList<MessageEvent>();
		while (it.hasNext()) {
			tmp.add(it.next());
			if (tmp.size() % 100000 == 0)
				System.err.println("Loading event: "+tmp.size());
		}
		log = tmp.toArray(new MessageEvent[0]);
		EventBase.sort(log);
	}

	/** Add a timestamp offset to all messages in the trace. */
	public void offset(long offset) {
		for (EventBase i : log) {
			i.setTimestamp(i.getTimestamp()+offset);
		}		
	}
	
	public MessageEvent get(int index) {
		return log[index];
	}
	
	/* TODO:Filter everything in the trace to only messages in a particular time range */
	
	
	/** Merges two logs together, permanently mutating BOTH logs */
	public void merge(EventTrace peer) {
		// We're merging in something empty.
		if (peer.log.length == 0)
			return;
		
		// We're empty, just copy the trace array.
		if (this.log.length == 0) {
			this.log = peer.log;
			peer.log = null; // Catch bugs.
			return;
		}
				
		Iterator<MessageEvent> j = this.iterator();
		Iterator<MessageEvent> k = peer.iterator();

		MessageEvent jm = j.next();
		MessageEvent km = k.next();

		int i=0;
		MessageEvent out[] = new MessageEvent[this.log.length+peer.log.length];
		
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
	public Iterator<MessageEvent> iterator() {
		return new Iter();
	}

	class Iter implements Iterator<MessageEvent> {
		int index = 0;
		@Override
		public boolean hasNext() {
			return index < log.length;
		}

		@Override
		public MessageEvent next() {
			if (index < log.length)
				return log[index++];
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/** Build a set of all of the recipient_host objects */
	public Set<Object> buildRecipientSet() {
		Set<Object> out = new HashSet<Object>();
		for (MessageEvent e : this) {
			out.add(e.getRecipientHost());
		}
		return out;
	}

	/** Build a set of all of the recipient_host senders */
	public Set<Object> buildSenderSet() {
		Set<Object> out = new HashSet<Object>();
		for (MessageEvent e : this) {
			out.add(e.getSenderHost());
		}
		return out;
	}

	public void keepOnlySender(Object sender) {
		ArrayList<MessageEvent> tmp = new ArrayList<MessageEvent>();
		for (MessageEvent e : this)
			if (sender.equals(e.getSenderHost()) && !e.getRecipientHost().equals(e.getSenderHost()))
				tmp.add(e);
		log = tmp.toArray(new MessageEvent[0]);
	}
	
	public void keepOnlyRecipient(Object recipient) {
		ArrayList<MessageEvent> tmp = new ArrayList<MessageEvent>();
		for (MessageEvent e : this)
			if (recipient.equals(e.getRecipientHost()) && !e.getRecipientHost().equals(e.getSenderHost()))
				tmp.add(e);
		log = tmp.toArray(new MessageEvent[0]);
	}

	final double EPOCHLENGTH = .100;
	
	/** Replay signers from a single source to many recipients, create a file of signed messages for each destination host -- USED TO BENCHMARK SIGNING. */
	void replaySign(QueueBase senderqueue, HashMap<Object,CodedOutputStream> streammap) {
		double epochend = log[0].getTimestamp() + EPOCHLENGTH ;
		for (MessageEvent e : log) {
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipientHost()));
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
		for (MessageEvent e : log) {
			OutgoingMessage msg = e.asOutgoingMessage(streammap.get(e.getRecipientHost()));
			QueueBase senderqueue = queuemap.get(e.getSenderHost());
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
	
	
	public void write(OutputStream output) throws IOException {
		Writer out=new BufferedWriter(new OutputStreamWriter(output));
		for (MessageEvent e : this)
			e.writeTo(out);
		out.flush();
	}
	
	/* Simulation infrastructure:
	 * 
	 *  When signing: 
	 *     We have one queue for each sender_host object; we model each sender_host independently and use a virtual clock, triggering process()'s when appropriate. 
	 *     Each destination is placed into a different output file. 
     *         This is done by instantiating OutgoingMessage with the correct outgoing stream.
	 *
     *     Each distinct signer has a separate queue for processing, and its own publickeyprims.
	 *
	 *  When verifying:
	 *
	 *     Given a message trace (containing messages to only one recipient_host). play it, verifying all signatures.
	 *     Process the queue per the virtual clock.
	 *     
	 *     Create 'PublicKeyPrims' objects containing the signer's public keys on-the-fly, using the information in the sender_id's in the message bodies. 
	 *     Record various statistics.
	 *  
	 *  */




}