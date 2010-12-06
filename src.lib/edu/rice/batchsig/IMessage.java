package edu.rice.batchsig;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public interface IMessage extends Message {
	/** Callback invoked to report on the signature validity */
	public void signatureValidity(boolean valid);

	/** Used to get the signature blob for a message during verification. */
	public TreeSigBlob getSignatureBlob();

	/** Used to get the author who signed this message. 
	 * 
	 *  Used to optimize trying to splice history trees together. If unknown, use a singleton object 
	 *  and splice-merging code will do the right thing using the tree_id.
	 *  Return value must be suitable for a key in a hash table */
	public Object getAuthor();	

}
