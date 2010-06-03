package org.rice.crosby.batchsig;


public interface SignaturePrimitives {

	/** Sign an input message in protobuf format. */
	byte[] sign(byte[] data);
	
	/** Verify the signature */
	boolean verify(byte [] data, byte[] sig);
}
