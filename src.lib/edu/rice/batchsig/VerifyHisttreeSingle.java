package edu.rice.batchsig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import edu.rice.historytree.HistoryTree;

public class VerifyHisttreeSingle extends VerifyHisttree {
	public VerifyHisttreeSingle(SignaturePrimitives signer) {
		super(signer);
	}

	/** Handle all messages with the same treeID */
	protected void processMessagesFromTree(ArrayList<IMessage> l) {
		for (IMessage m : l) {
			HistoryTree<byte[],byte[]> tree = parseHistoryTree(m);			
			m.signatureValidity(Verifier.checkLeaf(m,tree) && verifyHistoryRoot(m,tree));
		}
	}

}