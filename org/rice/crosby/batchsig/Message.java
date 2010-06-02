package org.rice.crosby.batchsig;

import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

public interface Message {
	/** Return the hash value associated with the message that is to be signed. */
	byte []getData();

	/** Return the recipient of this message. Used for building efficient history tree membership 
	 * proofs. Return value must be suitable as a key for a hash table. */
	public Object getRecipient();
	
	
	/** Callback invoked with the signed result. Used by the message signing thread to set the signature when it is computed. May be executed concurrently.
	 *
	 * @param message The protocol buffer message denoting the proof. May be null if proof generation failed.
	 * 
	 * */
	public void signatureResult(TreeSigBlob message);

	/** Used to get the signature blob for verifying signatures. */
	public TreeSigBlob getSigBlob();

	/** Used to get the signer. Return value must be suitable for a key in a hash table */
	public Object getSigner();

	/** Used to report on the signature validity */
	public void signatureValidity(boolean valid);
}
