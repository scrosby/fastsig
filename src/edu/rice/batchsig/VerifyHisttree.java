package edu.rice.batchsig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


import edu.rice.historytree.HistoryTree;
import edu.rice.historytree.NodeCursor;
import edu.rice.historytree.TreeBase;

public class VerifyHisttree extends VerifyHisttreeCommon {
	public VerifyHisttree(SignaturePrimitives signer) {
		super(signer);
	}

	
	public void add(Message m) {
		if (!messages.containsKey(m.getAuthor()))
			messages.put(m.getAuthor(),new ArrayList<Message>());
		messages.get(m.getAuthor()).add(m);
	}
		
	HashMap<Object,ArrayList<Message>> messages = new HashMap<Object,ArrayList<Message>>();

	public void finishBatch() {
		// Process each signer's list of messages in turn.
		for (ArrayList<Message> l : messages.values()) {
			processMessagesFromSigner(l);
			l.clear();
		}
	}
	
	
	//Handle the history queue.
	void processMessagesFromSigner(ArrayList<Message> l) {
		// Handle the easy case first when there's only one thing. Premature optimization??
		if (false && l.size() == 1) {
			// BROKEN CODE.
			Message m = l.get(0);
			m.signatureValidity(verifyHistoryRoot(m,parseHistoryTree(m)));
			return;
		}
		
		// Sort based on treeID first, then message index.
		Collections.sort(l,new Comparator<Message>(){
			public int compare(Message a, Message b) {
				long diff1 = a.getSignatureBlob().getTreeId()-b.getSignatureBlob().getTreeId();
				if (diff1 > 0) return 1; 
				if (diff1 < 0) return -1;
				return a.getSignatureBlob().getTree().getVersion()-b.getSignatureBlob().getTree().getVersion();
			}});

		// Now break it down into one-arraylist per tree.
		int i=0,j=1;
		ArrayList<Message> out;
		
		// Has to have at least one message.
		do {
			if (j == l.size() || l.get(i).getSignatureBlob().getTreeId() != l.get(j).getSignatureBlob().getTreeId()) {
				out = new ArrayList<Message>();
				out.addAll(l.subList(i,j));
				processMessagesFromTree(out);
				i=j;
			}
			j++;
		} while (i < l.size());
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
			//System.out.format("*Checking message at leaf %d\n",m.getSignatureBlob().getLeaf());
			
			boolean validated = false;
			HistoryTree<byte[],byte[]> tree = parseHistoryTree(m);
			
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
						//System.out.format("Using verified splice %d in tree version %d\n",version, latertree.version());
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
				//System.out.format("Splices do not have tree %d\n",version);
				if (verifyHistoryRoot(m,tree)) {
					validated = true; // GOOD signature.
				} else {
					System.out.println("Signature necessary, but doesn't validate. Skip message");
				}
			}

			// Put in a 'splice' for the tree's version
			if (validated && !splices.containsKey(version)) {
				//System.out.format("Store self-splice at %d with tree-version %d\n",tree.version(),version);
				splices.put(tree.version(), m);
				trees.put(m, tree);
			}
			
			// Save the splices, if any, of this message, if validated.
			if (validated && m.getSignatureBlob().getSpliceHintCount() > 0) {
					trees.put(m, tree);
					for (int splice: m.getSignatureBlob().getSpliceHintList()) {
						if (tree.leaf(splice) == null) {
							// Claims it has splice, but doesn't have the leaf.
							System.out.println("Claims splice, but no splice included.");
						} else {
							System.out.format("Store splice at %d with tree-version %d\n",splice,version);
							splices.put(splice,m);
						}
					}
			}
			// Now invoke the callback with the validity.
			m.signatureValidity(validated);
		}
	}
}
