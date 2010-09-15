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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.Verifier;



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
 * will validate M. M might have valid splices to prior messages, spliced to yet other messages.
 * With one public key verification on M, we can validate those prior messages through very cheap hash operations on spliced signatures.
 * 
 * This is not ideal. What would be more efficient would be to look for a later message P whose
 * transitive closure of splices includes M, then by validating P, we validate many more messages through cheap hash operations.
 * 
 * How to find P? 
 * 
 * Algorithm 1: Use Union-Find. 
 * 
 * Each group is a set of messages which can be validated by verifying the exlempar element's single public key signature. 
 * (This means that validate the signature splices BEFORE merging into a group).
 *
 * Given a new message M, the cases are:
 * 
 * If an existing message in a group includes a splice to M, validate the splice and add M to the group.
 * If M includes a splice the exlempar element of a group G, validate the splice and M becomes the exlempar of that group.
 * If M includes a splice to the exlempar elements of several groups, validate the splices and M becomes the exlempar of the union of the groups.
 *
 * PROBLEM: Not robust to signature validation failures.
 *
 * Algorithm 2: Store the entire graph
 *
 * Store a dag. Nodes in this dag correspond to messages, and edges correspond to splice points. Some nodes in this graph can be 'incomplete', meaning that we've not seen.
 * 
 * either actual message, or 'stub' messages. 
 * 
 * */



public class VerifyQueueLazy extends QueueBase {
	private Verifier verifier;
	private SignaturePrimitives signer;

	public VerifyQueueLazy(SignaturePrimitives signer) {
		super();
		if (signer == null)
			throw new NullPointerException();
		this.signer = signer;
		this.verifier = new Verifier(signer);
	}

	private HashMap<Object,HashMap<Long,OneTree>> map1 = new HashMap<Object,HashMap<Long,OneTree>>();
	/** When was this tree last used? */
	private HashMap<OneTree,Long> last = new HashMap<OneTree,Long>();
	private HashSet<OneTree> lastused = new HashSet<OneTree>();

	// First, process everything.
	public void process() {
		processQueue();
		processOneTrees();
	}
	
	public void force(Message m) {
		Object author = m.getAuthor();
		Long treeid = m.getSignatureBlob().getTreeId();
		HashMap<Long,OneTree> map2 = map1 != null ? map1.get(author) : null;
		OneTree tree = map2 != null ? map2.get(treeid) : null;
		if (tree == null) {
			System.out.println("Forcing message thats not in the tree??? Don't do anything.");
			return;
		}
		tree.force(m);
	}
	
	/** Tree last used longer than this ago gets everything forced. */
	final static long FORCE_DELAY1 = 20*60*1000;
	/** Anything older than this and not the most recently updated tree gets everything forced */
	final static long FORCE_DELAY2 = 2*60*1000;
	/** After doing all of the processing, decide if it is time to eagerly process all of the data */

	public void processOneTrees() {
		// For each host.
		for (Entry<Object, HashMap<Long, OneTree>> host : map1.entrySet()) {
			for (Entry<Long, OneTree> entry : host.getValue().entrySet()) {
				OneTree tree = entry.getValue();
				long now = System.currentTimeMillis();
				long limit;
				if (lastused.contains(tree))
					limit = FORCE_DELAY1;
				else
					limit = FORCE_DELAY2;
				
				if (now-last.get(tree) <  limit)
					// If this is the 'last' tree for this host, then do nothing;
					continue;
				//Eagerly process this tree .
				System.out.println("Forcing tree due to time limit");
				tree.forceAll();
				last.remove(tree);
				lastused.remove(tree);
			}
		}
	}

	/** Place all of the requested messages from the incoming queue into the for-lazy-processing OneTree objects. */
	public void processQueue() {
		ArrayList<Message> oldqueue = atomicGetQueue();
		long now = System.currentTimeMillis();

		// Go over each message, place it in the appropriate oneTree.
		for (Message m : oldqueue) {
			if (m == null) {
				System.err.println("Null message in queue?");
				continue;
			}
			Object author = m.getAuthor();
			Long treeid = m.getSignatureBlob().getTreeId();
			if (!map1.containsKey(author))
				map1.put(author,new HashMap<Long,OneTree>());
			HashMap<Long,OneTree> map2 = map1.get(author);
			if (!map2.containsKey(treeid))
				map2.put(treeid,new OneTree());
			OneTree tree = map2.get(treeid);
			tree.addMessage(m);
			last.put(tree,now);
			lastused.add(tree);
		}
	}
}

