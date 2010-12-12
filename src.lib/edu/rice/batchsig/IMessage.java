package edu.rice.batchsig;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;

/** Template representing an incoming message to be verified. */
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

	/** Which user is targetted by this message? */
	public Object getRecipientUser();

	
	
	/** Function to help in tracking the latency. 
	 * 
	 * Mark this message as not needing its latency measured, for instance, if it was verified idly. 
	 * */
	public void resetCreationTimeNull();

	/** Function to help in tracking the latency. 
	 * 
	 * Remember when the message was 'forced', so that we can figure out how long it took until it was later verified. */
	public void resetCreationTimeTo(long tstamp);

}
