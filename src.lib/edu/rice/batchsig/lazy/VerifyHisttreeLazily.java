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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.HistTreeTools;
import edu.rice.batchsig.bench.Tracker;

/**
 * Copy of verifyqueuelazy, which takes loginlogout hints and tries to delay
 * signature verification for spliced signatures.
 * 
 * Messages are only lazily verified, when explicitly forced.
 * 
 * When a public key signature is verified, that verification can validate both
 * that one message, and earlier messages through the transitive closure of
 * verified splices.
 * 
 * Lets say that message M is forced, we could check the signature on M, which
 * will validate M. M might have valid splices to prior messages, spliced to yet
 * other messages. With one public key verification on M, we can validate those
 * prior messages through very cheap hash operations on spliced signatures.
 * 
 * This is not ideal. What would be more efficient would be to look for a later
 * message P whose transitive closure of splices includes M, then by validating
 * P, we validate many more messages through cheap hash operations.
 * 
 * How to find P?
 * 
 * Algorithm 1: Use Union-Find.
 * 
 * Each group is a set of messages which can be validated by verifying the
 * exlempar element's single public key signature. (This means that validate the
 * signature splices BEFORE merging into a group).
 * 
 * Given a new message M, the cases are:
 * 
 * If an existing message in a group includes a splice to M, validate the splice
 * and add M to the group. If M includes a splice the exlempar element of a
 * group G, validate the splice and M becomes the exlempar of that group. If M
 * includes a splice to the exlempar elements of several groups, validate the
 * splices and M becomes the exlempar of the union of the groups.
 * 
 * PROBLEM: Not robust to signature validation failures.
 * 
 * Algorithm 2: Store the entire graph
 * 
 * Store a dag. Nodes in this dag correspond to messages, and edges correspond
 * to splice points. Some nodes in this graph can be 'incomplete', meaning that
 * we've not seen.
 * 
 * either actual message, or 'stub' messages.
 * 
 * NOT multithread safe.
 * 
 * */

public class VerifyHisttreeLazily implements
		VerifyLazily, WrappedIMessage.Callback {
	/**
	 * When there are more than MAX_TREES OneTree's, force all of the messages
	 * in the oldest tree
	 */
	private static int MAX_TREES = 100;

	/**
	 * When a tree has more than MAX_TREE_SIZE, force the oldest message in the
	 * oldest tree
	 */
	private static int MAX_TREE_SIZE = 1000;

	/** The signer. */
	final SignaturePrimitives signer;

	/** Map from recipient_user to the messages queued to that recipient_user. */
	Multimap<Object, IMessage> userToMessages = HashMultimap.create();

	/** Track the number of messages enqueued. */
	private AtomicInteger size = new AtomicInteger(0);

	/** Map from (author_server, treeid) -> OneTree. */
	private Table<Object, Long, OneTree> map1 = HashBasedTable.create();

	/** Track info for expiration. */
	private TreeExpirationManager expirationqueue = new TreeExpirationManager(MAX_TREES);

	/**
	 * The expiration manager callback receives trees that are need to be
	 * idle-forced. They get stored here so that idle forcing can force them. In
	 * particular, there are too many live trees and all messages here need to
	 * be forced.
	 */
	private HashSet<OneTree> treesToForceAll = new HashSet<OneTree>();

	/**
	 * The expiration manager callback receives trees that are need to be
	 * idle-forced. They get stored here so that idle forcing can force them. In
	 * particular, the oldest message in these trees should be forced.
	 */
	private HashSet<OneTree> treesToForceOne = new HashSet<OneTree>();

	/**
	 * Track the oldest OneTree's, so I know which ones have the oldest
	 * messages, for when I do forceOldest().
	 */
	@SuppressWarnings("serial")
	class TreeExpirationManager extends ExpirationManager<OneTree> {
		TreeExpirationManager(int size_limit) {
			super(size_limit);
		}

		@Override
		protected void expire(OneTree eldest) {
			System.out.println("Expiration for too many trees");
			treesToForceAll.add(eldest);
		}
	}

	public VerifyHisttreeLazily(SignaturePrimitives signer) {
		this.signer = signer;
	}
	
	/** This message has been validated, can stop tracking it now. */
	public void messageValidatorCallback(IMessage m, boolean valid) {
		userToMessages.remove(m.getRecipientUser(), m);
		size.decrementAndGet();
	}

	/** Given a message, get the OneTree responsible for managing it. */
	private OneTree getOneTreeForMessage(IMessage m) {
		return map1.get(m.getAuthor(), m.getSignatureBlob().getTreeId());
	}

	/** Given a message, get or make the OneTree responsible for managing it. */
	private OneTree makeOneTreeForMessage(IMessage m) {
		OneTree out = getOneTreeForMessage(m);
		if (out == null) {
			out = new OneTree(signer, m.getAuthor(), m.getSignatureBlob()
					.getTreeId());
			map1.put(m.getAuthor(), m.getSignatureBlob().getTreeId(), out);
		}
		return out;
	}

	/**
	 * Done at the end of every batch and tries to lazy force any old and
	 * expired messages.
	 */
	public void doExpire() {
		// System.out.println("  Forcing batch begin");
		for (OneTree i : treesToForceAll) {
			i.forceAll();
			map1.remove(i.getAuthor(), i.getTreeid());
		}
		for (OneTree i : treesToForceOne)
			if (!treesToForceAll.contains(i))
				i.forceOldest();
		treesToForceAll.clear();
		treesToForceOne.clear();
		// System.out.println("  Forcing batch end");
	}

	/** Force the given message that was previously enqueued with add(IMessage). */
	public void force(IMessage m) {
		OneTree tree = getOneTreeForMessage(m);
		if (tree == null) {
			System.out
					.println("Forcing message thats not in the tree??? Don't do anything.");
			return;
		}
		tree.forceMessage(m);
	}

	@Override
	public void forceUser(Object user, long timestamp) {
		while (true) {
			Collection<IMessage> ml = userToMessages.get(user);
			if (ml.isEmpty())
				return;
			IMessage m = ml.iterator().next();
			// System.out.format("Forcing user %s at %d was %d  -- %s\n",user.toString(),timestamp,m.getCreationTime(),m.toString());
			m.resetCreationTimeTo(timestamp);
			// System.out.format("For forced user %s, found message %s\n",user.toString(),m.toString());
			force(m);
		}
	}

	@Override
	public void forceAll() {
		for (OneTree tree : map1.values())
			tree.forceAll();
	}

	@Override
	public void forceOldest() {
		// Nothing outstanding to do.
		if (expirationqueue.size() == 0)
			return;
		// Find the oldest.
		OneTree x = expirationqueue.keySet().iterator().next();
		Tracker.singleton.idleforces++;
		if (x == null)
			throw new Error("Expiration queue weirdness");
		if (x.forceOldest())
			return;
		// If the onetree is empty, remove this from the expiration queue
		// entirely.
		expirationqueue.remove(x);
	}

	@Override
	public void add(IMessage m) {
		size.incrementAndGet();
		OneTree tree = this.makeOneTreeForMessage(m);
		tree.addMessage(m);
		if (tree.size() > MAX_TREE_SIZE) {
			treesToForceOne.add(tree);
			System.out.println("Expiration for too big tree.");
		}
		expirationqueue.put(tree, tree);
		userToMessages.put(m.getRecipientUser(), m);
		doExpire();
	}

	@Override
	public int peekSize() {
		return size.get();
	}
}
