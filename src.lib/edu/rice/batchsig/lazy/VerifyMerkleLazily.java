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

package edu.rice.batchsig.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.VerifyMerkle;
import edu.rice.batchsig.bench.Tracker;


/** Class for lazily verifying Merkle signatures. */
public class VerifyMerkleLazily implements VerifyLazily, WrappedIMessage.Callback {
	/** Maximum number of outstanding unverified messages before we force the oldest. */
	private static final int MAX_MESSAGES = 20000;

	/** Track info for expiration. */
	private TreeExpirationManager expirationqueue = new TreeExpirationManager(MAX_MESSAGES);

	/** Underlying eager Merkle signature verifier. */
	private final VerifyMerkle merkleverify;

	/** Track the number of messages enqueued. */
	private AtomicInteger size = new AtomicInteger(0);

	/** Map from recipient_user to the messages queued to that recipient_user. */
	Multimap<Object, IMessage> userToMessages = HashMultimap.create();
	
	/** Which messages are asynchronously marked as needing to be lazily forced. */
	private ArrayList<IMessage> expired = new ArrayList<IMessage>();
	
	public VerifyMerkleLazily(SignaturePrimitives signer) {
		merkleverify = new VerifyMerkle(signer);
	}

	
	/** This message has been validated, and we can stop tracking it now. */
	public void messageValidatorCallback(IMessage m, boolean valid) {
		userToMessages.remove(m.getRecipientUser(), m);
		size.decrementAndGet();
	}
		

	/** Track the oldest unverified Merrkle signature */
	@SuppressWarnings("serial")
	class TreeExpirationManager extends ExpirationManager<IMessage> {
		TreeExpirationManager(int size_limit) {
			super(size_limit);
		}
		
		@Override
		protected void expire(IMessage eldest) {
			System.out.println("Expiration for too many trees");
			// Enqueue it in the underlying verifier
			merkleverify.add(eldest);
		}
	}
	
	@Override
	public void forceUser(Object user, long timestamp) {
		Collection<IMessage> ml = new ArrayList<IMessage>(userToMessages.get(user));
		for (IMessage m : ml) {
			//System.out.println("Forcing "+user + "   " +  m.getVirtualClock());
			m.resetCreationTimeTo(timestamp);
			merkleverify.add(m);
			expirationqueue.remove(m);
		}
	}
	
	@Override
	public void forceAll() {
		for (IMessage m : expired) {
			m.resetCreationTimeNull();
			merkleverify.add(m); // Process eldest.
		}
		for (IMessage m : new ArrayList<IMessage>(expirationqueue.keySet())) {
			m.resetCreationTimeNull();
			merkleverify.add(m);
		}
	}

	@Override
	public void forceOldest() {
		if (expired.size() > 0) {
			Iterator<IMessage> i = expired.iterator();
			IMessage m = i.next();
			m.resetCreationTimeNull();
			merkleverify.add(m);
			i.remove();
		}
			
		// Keep on trying until we expire an entry, if any exists.
		if (expirationqueue.size() == 0)
			return;
		IMessage x = expirationqueue.keySet().iterator().next();
		Tracker.singleton.idleforces++;
		if (x == null)
			throw new Error("Expiration queue weirdness");

		merkleverify.add(x);		
		expirationqueue.remove(x);
	}
	
	@Override
	public void add(IMessage m) {
		WrappedIMessage msg = new WrappedIMessage(m);
		msg.setCallback(this);
		size.incrementAndGet();
		//if (tree.size() > MAX_TREE_SIZE) {
		//	System.out.println("Expiration for too big tree.");
		//}
		expirationqueue.put(msg,msg);
		userToMessages.put(msg.getRecipientUser(), m);
	}

	@Override
	public int peekSize() {
		return size.get();
	}
	
}

