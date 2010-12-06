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

package edu.rice.batchsig.splice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.Message;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.VerifyHisttreeCommon;
import edu.rice.batchsig.VerifyMerkle;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.Tracker;
import edu.rice.batchsig.bench.log.MultiplexedPublicKeyPrims;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;



public class VerifyMerkleLazily implements VerifyLazily {
	public static int MAX_MESSAGES = 100000;
	final public VerifyMerkle merkleverify;
	AtomicInteger size = new AtomicInteger(0);
	
	public VerifyMerkleLazily(MultiplexedPublicKeyPrims signer) {
		merkleverify = new VerifyMerkle(signer);
	}

	
	/** THis message has been validated, can stop tracking it now. */
	public void messageValidatorCallback(IncomingMessage m) {
		userToMessages.remove(m.getRecipientUser(),m);
		size.decrementAndGet();
	}
		
	/** Track info for expiration */
	private TreeExpirationManager expirationqueue = new TreeExpirationManager(MAX_MESSAGES);

	// NOT USED RIGHT NOW
	private ArrayList<IncomingMessage> expired = new ArrayList<IncomingMessage>();
	
	class TreeExpirationManager extends ExpirationManager<IncomingMessage> {
		private static final long serialVersionUID = 1L;

		TreeExpirationManager(int size_limit) {
			super(size_limit);
		}
		
		protected boolean removeEldestEntry(Map.Entry<IncomingMessage,IncomingMessage> eldest) {
			if (super.removeEldestEntry(eldest)) {
				System.out.println("Expiration for too many trees");
				merkleverify.add(eldest.getKey());
				return true;
			}
			return false;
		}
	}
	
	/** Map from recipient_user to the messages queued to that recipient_user */
	Multimap<Object,IncomingMessage> userToMessages = HashMultimap.create();

	
	public void force(IncomingMessage m) {
		throw new Error("Not actually used");
		/* merkleverify.add(m); */
	}
	
	public void forceUser(Object user, long timestamp) {
		Collection<IncomingMessage> ml = new ArrayList<IncomingMessage>(userToMessages.get(user));
		for (IncomingMessage m : ml) {
			//System.out.println("Forcing "+user + "   " +  m.getVirtualClock());
			m.resetCreationTimeTo(timestamp);
			merkleverify.add(m);
			expirationqueue.remove(m);
		}
	}
	
	public void forceAll() {
		for (IncomingMessage m : expired) {
			m.resetCreationTimeNull();
			merkleverify.add(m); // Process eldest.
		}
		for (IncomingMessage m : new ArrayList<IncomingMessage>(expirationqueue.keySet())) {
			m.resetCreationTimeNull();
			merkleverify.add(m);
		}
	}
	
	public void forceOldest() {
		if (expired.size() > 0) {
			Iterator<IncomingMessage> i = expired.iterator();
			IncomingMessage m = i.next();
			m.resetCreationTimeNull();
			merkleverify.add(m);
			i.remove();
		}
			
		// Keep on trying until we expire an entry, if any exists.
		if (expirationqueue.size() == 0)
			return;
		IncomingMessage x = expirationqueue.keySet().iterator().next();
		Tracker.singleton.idleforces++;
		if (x == null)
			throw new Error("Expiration queue weirdness");

		merkleverify.add(x);		
		expirationqueue.remove(x);
	}
	
	
	public void add(IMessage m) {
		add((IncomingMessage)m);
	}

	public void add(IncomingMessage m) {
		m.registerValidator(this);
		size.incrementAndGet();
		//if (tree.size() > MAX_TREE_SIZE) {
		//	System.out.println("Expiration for too big tree.");
		//}
		expirationqueue.put(m,m);
		userToMessages.put(m.getRecipientUser(), m);
	}

	/** Get the size of the queue. May be called concurrently from any number of threads */
	public int peekSize() {
		return size.get();
	}
	
}

