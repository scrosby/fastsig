package edu.rice.batchsig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


import edu.rice.historytree.NodeCursor;
import edu.rice.historytree.TreeBase;

public abstract class VerifyHisttree extends VerifyHisttreeCommon {
	/** Map from (author_server, treeid) -> OneTree */
	private Table<Object,Long,ArrayList<Message>> map1 = HashBasedTable.create();

	ArrayList<Message> getListForMessage(Message m) {
		ArrayList<Message> out = map1.get(m.getAuthor(),m.getSignatureBlob().getTreeId());
		if (out == null) {
			out = new ArrayList<Message>();
			map1.put(m.getAuthor(),m.getSignatureBlob().getTreeId(),out);
		}
		return out;
	}
	
	
	public VerifyHisttree(SignaturePrimitives signer) {
		super(signer);
	}

	
	public void add(Message m) {
		getListForMessage(m).add(m);
	}
		
	HashMap<Object,ArrayList<Message>> messages = new HashMap<Object,ArrayList<Message>>();

	public void finishBatch() {
		// Process each signer's list of messages in turn.
		for (ArrayList<Message> l : map1.values())
			processMessagesFromTree(l);
		map1.clear();
	}
	
	abstract protected void processMessagesFromTree(ArrayList<Message> l);
}
