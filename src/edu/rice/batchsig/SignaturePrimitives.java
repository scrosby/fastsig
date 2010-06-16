package edu.rice.batchsig;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;


public interface SignaturePrimitives {

	/** Sign an input message in protobuf format. Should set the following fields in the protobuf:
	 * 
	 *   optional bytes signature_bytes;
	 *   optional bytes signer_id;
     *   optional SignatureAlgorithm signature_algorithm;
	 * */
	void sign(byte[] data, TreeSigBlob.Builder out);
	
	/** Verify the signature */
	boolean verify(byte [] data, TreeSigBlob sig);
}
