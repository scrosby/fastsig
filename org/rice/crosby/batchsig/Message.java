package org.rice.crosby.batchsig;

import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

public interface Message {
	/** Return the hash value associated with the message that is to be signed. The returned data 
	 * *is* stored in the history tree. */
	byte []getData();

	/** Callback invoked with the signed result. Used by the message signing thread to set the signature when it is computed. May be executed concurrently.
	 *
	 * @param message The protocol buffer message denoting the proof. May be null if proof generation failed.
	 * 
	 * */
	public void signatureResult(TreeSigBlob message);

	/** Callback invoked to report on the signature validity */
	public void signatureValidity(boolean valid);

	/** Used to get the signature blob for a message during verification. */
	public TreeSigBlob getSignatureBlob();
	
	// Callbacks used for generating and using splices in the history tree.
	
	/** Return the recipient of this message. 
	 * 
	 * Used for setting the splice points for history tree membership 
	 * proofs that support splicing. Return value must be suitable as a key for a hash table. 
	 * If no recipient, return a new'ed Object(). */
	public Object getRecipient();
		
	
	/** Used to get the author who signed this message. 
	 * 
	 *  Used to optimize trying to splice history trees together. If unknown, use a singleton object 
	 *  and splice-merging code will do the right thing using the tree_id.
	 *  Return value must be suitable for a key in a hash table */
	public Object getAuthor();
	
	
	
}
