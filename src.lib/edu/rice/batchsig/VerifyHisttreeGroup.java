package edu.rice.batchsig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import edu.rice.historytree.HistoryTree;

/**
 * Verify all of the spliced signed messages in the queue while try to exploit
 * available splices. Exploit any splices that happen to be between messages in
 * the queue.
 */
public class VerifyHisttreeGroup extends VerifyHisttreeEagerlyBase {
	public VerifyHisttreeGroup(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	protected void process(ArrayList<IMessage> l) {
		// Traverse in reverse order from most recent to earliest.
		Collections.reverse(l);

		/*
		 * Algorithm: Traverse from the latest message to the earliest.
		 * 
		 * For each message, we see if we have a splice from a previously
		 * verified message (a reverse-index of splicepoints to the
		 * corresponding messages is stored in splices) If so, then verify the
		 * splice. If not, or a bad splice check the signature. If a message
		 * validates via one of these mechanisms, then this message is valid and
		 * add its splicepoints to splices.
		 */

		/**
		 * For each version number a confirmed valid message that claims to
		 * splice the requested message
		 */
		HashMap<Integer, IMessage> splices = new HashMap<Integer, IMessage>();

		/** Cache of the parsed trees */
		HashMap<IMessage, HistoryTree<byte[], byte[]>> trees = new HashMap<IMessage, HistoryTree<byte[], byte[]>>();

		for (IMessage m : l) {
			// System.out.format("*Checking message at leaf %d\n",m.getSignatureBlob().getLeaf());

			boolean validated = false;
			HistoryTree<byte[], byte[]> tree = HistTreeTools
					.parseHistoryTree(m);

			if (!Verifier.checkLeaf(m, tree)) {
				m.signatureValidity(false);
				continue;
			}

			int version = tree.version();
			// See if this message can be spliced on to something we already
			// know about.
			if (splices.containsKey(version)) {
				IMessage latermsg = splices.get(version);
				HistoryTree<byte[], byte[]> latertree = trees.get(latermsg);
				// Confirm the splice.
				if (Arrays.equals(latertree.aggV(version), tree.agg())) {
					// Splice is good! Is the message validated?
					if (Verifier.checkLeaf(m, tree)) {
						// And so is the message in it!
						// System.out.format("Using verified splice %d in tree version %d\n",version,
						// latertree.version());
						validated = true;
					} else {
						System.out
								.println("Broken proof that doesn't validate message in proof.");
					}
				} else {
					// BAD SPLICE.
					System.out
							.println("Bad Splice: Did history replay? Skipped messagae");
					// But see if we can check the signature.
				}
			}
			// No splice or invalid splice.
			if (validated == false) {
				// System.out.format("Splices do not have tree %d\n",version);
				if (HistTreeTools.verifyHistoryRoot(signer, m, tree)) {
					validated = true; // GOOD signature.
				} else {
					System.out
							.println("Signature necessary, but doesn't validate. Skip message");
				}
			}

			// Put in a 'splice' for the tree's version
			if (validated && !splices.containsKey(version)) {
				// System.out.format("Store self-splice at %d with tree-version %d\n",tree.version(),version);
				splices.put(tree.version(), m);
				trees.put(m, tree);
			}

			// Save the splices, if any, of this message, if validated.
			if (validated && m.getSignatureBlob().getSpliceHintCount() > 0) {
				trees.put(m, tree);
				for (int splice : m.getSignatureBlob().getSpliceHintList()) {
					if (tree.leaf(splice) == null) {
						// Claims it has splice, but doesn't have the leaf.
						// System.out.println("Claims splice, but no splice included.");
					} else {
						// System.out.format("Store splice at %d with tree-version %d\n",
						// splice, version);
						splices.put(splice, m);
					}
				}
			}
			// Now invoke the callback with the validity.
			m.signatureValidity(validated);
		}
	}

}
