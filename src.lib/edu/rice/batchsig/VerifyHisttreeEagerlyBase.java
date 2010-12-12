package edu.rice.batchsig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


import edu.rice.historytree.NodeCursor;
import edu.rice.historytree.TreeBase;

/** Abstract class for verifying a history tree eagerly. I.e., Whenever the queue is processed, the entire batch is processed and verified.
 * */
public abstract class VerifyHisttreeEagerlyBase extends Verifier {
	/** We don't bother to splices messages created in different history trees. Track each of the distinct history trees here.
	 * 
	 * Map from (author_server, treeid) -> OneTree */
	private Table<Object,Long,ArrayList<IMessage>> map1 = HashBasedTable.create();

	/** Get the appropriate list managing the history tree for a given message */
	private ArrayList<IMessage> getListForMessage(IMessage m) {
		ArrayList<IMessage> out = map1.get(m.getAuthor(),m.getSignatureBlob().getTreeId());
		if (out == null) {
			out = new ArrayList<IMessage>();
			map1.put(m.getAuthor(),m.getSignatureBlob().getTreeId(),out);
		}
		return out;
	}
	
	
	public VerifyHisttreeEagerlyBase(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	public void add(IMessage m) {
		getListForMessage(m).add(m);
	}
		
	@Override
	public void process() {
		// Process each signer's list of messages in turn.
		for (ArrayList<IMessage> l : map1.values())
			process(l);
		map1.clear();
	}
	/** Process all of the messages from one distinct history tree instance. */
	protected abstract void process(ArrayList<IMessage> l);
}
