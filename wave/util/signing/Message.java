package wave.util.signing;

import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

public interface Message {
	/** Return the hash value associated with the message that is to be signed. */
	byte []getData();
	
	/** Used by the message signing thread to set the signature when it is computed
	 * 
	 * Note, may be executed concurrently.
	 *
	 * @param message The protocol buffer message denoting the proof. May be null if proof generation failed.
	 * 
	 * */
	void setSigBlob(TreeSigBlob message);

	/** Used to get the signature blob for verifying signatures. */
	TreeSigBlob getSigBlob();
}
