package edu.rice.batchsig;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;

/** Interface for representing outgoing messages to be signed. */
public interface OMessage extends Message {
	/** Callback invoked with the signed result. Used by the message signing thread to set the signature when it is computed. May be executed concurrently.
	 *
	 * @param message The protocol buffer message denoting the proof. May be null if proof generation failed.
	 * 
	 * */
	public void signatureResult(TreeSigBlob message);

	
	// Callbacks used for generating and using splices in the history tree.
	
	/** Return the recipient_host of this message. 
	 * 
	 * Used for setting the splice points for history tree membership 
	 * proofs that support splicing. Return value must be suitable as a key for a hash table. 
	 * If no recipient_host, return a new'ed Object(). */
	public Object getRecipient();
		
	

}