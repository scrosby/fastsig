package edu.rice.batchsig;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import edu.rice.batchsig.Dag.DagNode;
import edu.rice.historytree.HistoryTree;

/** Represent all of the message from one history tree instance */
public class OneTree {
	private Verifier verifier;
	private SignaturePrimitives signer;
	/** For each version number the message at that number. */
	HashMap<Integer,Message> messages = new HashMap<Integer,Message>();

	/** Cache of unvalidated root hashes of the history trees for messages in messages. msg.getTree().agg()
	 *    When we verify a splice, we need to take the child message's agg() and compare it to aggV(child.version).
	 *    Rather than rebuild the child's pruned tree, or cache the whole thing, just cache the parts we need, the agg().
	 *  */
	HashMap<Message,byte[]> roothashes;


	/** For various message indices, cache of the validated root hashes of history trees; if a new message arrives with a hash in this list, then the message is validated without a public key op. */
	HashMap<Integer,byte[]> validatedhashes;
	
	/**
	 * Invariant; The dag contains nodes for each message and the version
	 * numbers of the splicepoints for each message.
	 * 
	 * If an edge is in the dag and both messages have been seen (are in the
	 * messages hash), then the splice has been VERIFIED.
	 * 
	 * If an edge is in the dag, but either the from or to messages in the
	 * dag are *NOT* in the messages hash, then the splice is provisional
	 * and may not exist. We wait until the peer message arrives, verify the
	 * splice, and keep the edge if the splice verifies, or remove the edge
	 * if the splice fails.
	 * 
	 * */
	Dag<Integer> dag;

	Dag<Integer>.DagNode getNode(Message m) {
		Integer key = m.getSignatureBlob().getLeaf();
		return dag.makeOrGet(key);
	}

	void addMessage(Message m) {
		Integer key = m.getSignatureBlob().getLeaf();
		HistoryTree<byte[],byte[]> tree = Verifier.parseHistoryTree(m);

		// First, see if this message is well-formed.
		if (!Verifier.checkLeaf(m,tree)) {
			System.out.println("Broken proof that doesn't validate message in proof.");
		}
		
		// First: Check to see if this message is in the validated hashes.
		if (validatedhashes.get(key) != null && Arrays.equals(tree.agg(),validatedhashes.get(key))) {
			// Already done!
			m.signatureValidity(true);
			return;
		}
		
		// Nope. Now enter the core processing loop.
		
		if (messages.containsKey(key)) {
			// PROBLEM: Have a message with this version number already.
			// TODO: Can't handle these at all. Only solution: Verify it immediately.
			throw new Error("TODO");
		}

		// By definition, at this point, we have never seen this message before.

		Dag<Integer>.DagNode node = getNode(m);
		
		// PART 1: See if we've seen later messages we might splice into.
		// This case should be rare and only occur when messages arrive out-of-order.
		for (Dag<Integer>.DagNode succ : node.getParents()) {
			// For each later message in the dag that provisionally splices this message.
			Integer succi = succ.get();
			Message succm = messages.get(succi);
			if (succm == null)
				throw new Error("Algorithm bug.");
			// Time to verify the splice is OK. 
			HistoryTree<byte[],byte[]> succtree = Verifier.parseHistoryTree(succm);
			if (Arrays.equals(succtree.aggV(key.intValue()),tree.agg())) {
				; // Splice verifies. Nothing to be done.
			} else {
				// Splice fails. Remove the edge.
				dag.removeEdge(succ,node);
			}
		}
			
		// PART 2: See which prior messages we splice into.
		for (Integer predi : m.getSignatureBlob().getSpliceHintList()) {
			// For each splicepoint to prior messages in this message,
			Message predm = messages.get(predi);
			Dag<Integer>.DagNode prednode;
			// Have we seen the prior message?
			if (predm == null) {
				// Nope. Add the node to the dag. Add an edge to that child; it'll be provisional
				prednode = dag.makeOrGet(predi);
				dag.addEdge(node,prednode);
			} else {
				// We have seen that message. We need to check the splice.
				if (Arrays.equals(tree.aggV(predi.intValue()),roothashes.get(predm))) {
					; // Splice verifies. Nothing to be done.
				} else {
					// Splice fails. Remove the edge.
					prednode = dag.makeOrGet(predi);
					dag.removeEdge(node,prednode);
				}
			}
		}
		roothashes.put(m,tree.agg());
		messages.put(key, m);
	}

	private void remove(Message m) {
		messages.remove(m.getSignatureBlob().getLeaf());
		roothashes.remove(m);
		getNode(m).remove();
	}
	
	
	void force(Message m) {
		Dag<Integer>.DagNode node = getNode(m);

		// Step one: Find a root.
		Dag<Integer>.Path rootPath = dag.rootPath(node);
		// Step two, until we find a root whose signature verifies.
		while (true) {
			Dag<Integer>.DagNode root = rootPath.root();
			Integer rooti = root.get();
			Message rootm = messages.get(rooti);
			HistoryTree<byte[],byte[]> roottree = Verifier.parseHistoryTree(rootm);

			// Verify the root's public key signature.
			if (verifier.verifyHistory(rootm,roottree)) {
				// Success!
				// Now traverse *all* descendents. 
				Collection<Dag<Integer>.DagNode> descendents = dag.getAllChildren(root);
				for (Dag<Integer>.DagNode i : descendents) {
					//Integer desci = i.get();
					Message descm = messages.get(i);
					if (descm != null) {
						// This message is not provisional. It is valid.
						descm.signatureValidity(true);
						// TODO: Cache the spliced predecessor hashes from this node as being valid?
						// Remove it from further tracking.
						remove(descm);
					}
				}
			} else {
				rootm.signatureValidity(false);
				remove(rootm);
			}
				
			// Failure. Try the next root.
			rootPath.next();
		}
		
		
	}
	void forceAll() {
		while (!messages.isEmpty())
			force(messages.entrySet().iterator().next().getValue());
	}
	
}