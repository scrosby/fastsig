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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.VerifyHisttreeCommon;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;



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



public class VerifHisttreeLazily extends VerifyHisttreeCommon {
	public VerifHisttreeLazily(SignaturePrimitives signer) {
		super(signer);
	}
	
	/** Handle all of the cases of removing a message from... everywhere. */
	public void removeMessage(IncomingMessage m) {
		userToMessages.remove(m.getRecipientUser(),m);
	}
		
	/** Map from (author_server, treeid) -> OneTree */
	private Table<Object,Long,OneTree> map1 = HashBasedTable.create();

	/** Track info for expiration */
	//private HashMap<OneTree,Long> last = new HashMap<OneTree,Long>();
	//private HashSet<OneTree> lastused = new HashSet<OneTree>();

	/** Map from recipient recipient_user to the messages queued to that recipient_user */
	Multimap<Object,Message> userToMessages = HashMultimap.create();
	
	
	OneTree getOneTreeForMessage(IncomingMessage m) {
		return map1.get(m.getAuthor(),m.getSignatureBlob().getTreeId());
	}
	OneTree forceGetOneTreeForMessage(IncomingMessage m) {
		OneTree out = getOneTreeForMessage(m);
		if (out == null) {
			out = new OneTree(this);
			map1.put(m.getAuthor(),m.getSignatureBlob().getTreeId(),out);
		}
		return out;
	}
	
	/** At the end of a batch of inserts, handle expiration forcing */
	public void finishBatch() {
		// Handle timeouts.
		/*
		for (Cell<Object, Long, OneTree> cell : map1.cellSet()) {
			Object host = cell.getRowKey();
			OneTree tree = cell.getValue();
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
		 */
	}
	
	public void force(IncomingMessage m) {
		OneTree tree = getOneTreeForMessage(m);
		if (tree == null) {
			System.out.println("Forcing message thats not in the tree??? Don't do anything.");
			return;
		}
		tree.forceMessage(m);
	}
	
	/** Tree last used longer than this ago gets everything forced. */
	final static long FORCE_DELAY1 = 20*60*1000;
	/** Anything older than this and not the most recently updated tree gets everything forced */
	final static long FORCE_DELAY2 = 2*60*1000;
	/** After doing all of the processing, decide if it is time to eagerly process all of the data */

	public void add(Message m) {
		add((IncomingMessage)m);
	}

	public void add(IncomingMessage m) {
		if (m == null) {
			System.err.println("Null message in queue?");
			return;
		}
		OneTree tree = this.getOneTreeForMessage(m);
		tree.addMessage(m);
		//last.put(tree,now);
		//lastused.add(tree);
	}
}

