package org.rice.crosby.batchsig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.rice.crosby.historytree.HistoryTree;

public class VerifyQueue extends QueueBase {
	private Verifier verifier;

	public VerifyQueue(SignaturePrimitives signer) {
		super();
		this.verifier = new Verifier(signer);
	}
	
	public void process() {
		ArrayList<Message> oldqueue = atomicGetQueue();

		HashMap<Object,ArrayList<Message>> messages = new HashMap<Object,ArrayList<Message>>();

		// Place into a hash table, one for each signer.
		for (Message m : oldqueue) {
			if (!messages.containsKey(m.getSigner()))
				messages.put(m.getSigner(),new ArrayList<Message>());
			messages.get(m.getSigner()).add(m);
		}
		
		// Process each signer's list of messages in turn.
		for (ArrayList<Message> l : messages.values()) {
			processMessagesFromSigner(l);

		}
	}
	
	void processMessagesFromSigner(ArrayList<Message> l) {
		// Handle the easy case first when there's only one thing. Premature optimization??
		if (false && l.size() == 1) {
			Message m = l.get(0);
			m.signatureValidity(verifier.verifyHistory(m,Verifier.parseHistoryTree(m)));
			return;
		}
		
		// Sort based on treeID first, then message index.
		Collections.sort(l,new Comparator<Message>(){
			public int compare(Message a, Message b) {
				long diff1 = a.getSigBlob().getTreeId()-b.getSigBlob().getTreeId();
				if (diff1 > 0) return 1; 
				if (diff1 < 0) return -1;
				return a.getSigBlob().getTree().getVersion()-b.getSigBlob().getTree().getVersion();
			}});

		// Now break it down into one-arraylist per tree.
		int i=0,j;
		ArrayList<Message> out;
		for (j = 1 ; j < l.size(); j++) {
			if (l.get(i).getSigBlob().getTreeId() != l.get(i).getSigBlob().getTreeId()) {
				out = new ArrayList<Message>();
				out.addAll(l.subList(i,j));
				processMessagesFromTree(out);
				i=j;
			}
		}
		out = new ArrayList<Message>();
		out.addAll(l.subList(i,j));
		processMessagesFromTree(out);
	}
	
	/** Handle all messages with the same treeID */
	void processMessagesFromTree(ArrayList<Message> l) {
		// Traverse in reverse order from most recent to earliest.
		Collections.reverse(l);

		/* Algorithm: Traverse from the latest message to the earliest. 
		 * 
		 * For each message, we see if we have a splice from a previously verified message (a reverse-index of splicepoints to the corresponding messages is stored in splices)
	     * If so, then verify the splice. If not, or a bad splice check the signature. 
	     * If a message validates via one of these mechanisms, then this message is valid and add its splicepoints to splices.
		 */
		
		/** For each version number a confirmed valid message that claims to splice the requested message */
		HashMap<Integer,Message> splices = new HashMap<Integer,Message>();

		/** Cache of the parsed trees */
		HashMap<Message,HistoryTree<byte[],byte[]>> trees = new HashMap<Message,HistoryTree<byte[],byte[]>>();
		
		for (Message m : l) {
			boolean validated = false;
			HistoryTree<byte[],byte[]> tree = Verifier.parseHistoryTree(m);
			
			int version = tree.version();
			// See if this message can be spliced on to something we already know about.
			if (splices.containsKey(version)) {
				Message latermsg = splices.get(version);
				HistoryTree<byte[],byte[]> latertree = trees.get(latermsg);
				// Confirm the splice.
				if (Arrays.equals(latertree.aggV(version),tree.agg())) {
					// Splice is good! Is the message validated?
					if (Verifier.checkLeaf(m,tree)) {
						// And so is the message in it!
						validated = true;
					} else {
						System.out.println("Broken proof that doesn't validate message in proof.");
					}
				} else {
					// BAD SPLICE. 
					System.out.println("Bad Splice: Did history replay? Skipped messagae");
					// But see if we can check the signature.
				}
			}
			// No splice or invalid splice.
			if (validated == false) {
				if (verifier.verifyHistory(m,tree)) {
					validated = true; // GOOD signature.
				} else {
					System.out.println("Signature necessary, but doesn't validate. Skip message");
				}
			}

			// Save the splices, if any, of this message, if validated.
			if (validated && m.getSigBlob().getSpliceHintCount() > 0) {
					trees.put(m, tree);

					for (int splice: m.getSigBlob().getSpliceHintList()) {
						if (tree.leaf(splice) == null) {
							// Claims it has splice, but doesn't have the leaf.
							System.out.println("Claims splice, but no splice included.");
						} else {
							splices.put(splice,m);
						}
					}
			}
			// Now invoke the callback with the validity.
			m.signatureValidity(validated);
		}
	}
}

