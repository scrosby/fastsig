package edu.rice.batchsig;

import java.util.ArrayList;
import edu.rice.historytree.HistoryTree;


/** Verify each messages in the batch one at a time. */
public class VerifyHisttreeSingle extends VerifyHisttreeEagerlyBase {
	public VerifyHisttreeSingle(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	protected void process(ArrayList<IMessage> l) {
		for (IMessage m : l) {
			HistoryTree<byte[], byte[]> tree = parseHistoryTree(m);			
			m.signatureValidity(Verifier.checkLeaf(m, tree) && verifyHistoryRoot(m,tree));
		}
	}
}