/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.batchsig;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public interface Message {
	/** Return the hash value associated with the message that is to be signed. The returned data 
	 * *is* stored in the history tree, so should probably be a hash of the actual underlying data being authenticated. */
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
	
	/** Return the recipient_host of this message. 
	 * 
	 * Used for setting the splice points for history tree membership 
	 * proofs that support splicing. Return value must be suitable as a key for a hash table. 
	 * If no recipient_host, return a new'ed Object(). */
	public Object getRecipient();
		
	
	/** Used to get the author who signed this message. 
	 * 
	 *  Used to optimize trying to splice history trees together. If unknown, use a singleton object 
	 *  and splice-merging code will do the right thing using the tree_id.
	 *  Return value must be suitable for a key in a hash table */
	public Object getAuthor();	
}
