package wave.util.signing;

import com.google.protobuf.MessageLite;

public interface Signer {

	/** Sign an input message in protobuf format. */
	byte[] sign(byte[] data);
	
	/** Verify the signature */
	boolean verify(byte [] data, byte[] sig);
}
