package org.crosby.batchsig.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.rice.crosby.batchsig.SignaturePrimitives;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob.Builder;

import com.google.protobuf.ByteString;


/** 'Sign' a string by computing a digest over it. 
 * 
 * Use this for testing. 
 * For testing.
 * 
 * @author crosby
 *
 */
public class DigestPrimitive implements SignaturePrimitives {

	public byte[] hash(byte[] data) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			md.update(data);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void sign(byte[] data, Builder out) {
		out.setSignatureBytes(ByteString.copyFrom(hash(data)));
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		return Arrays.equals(hash(data),sig.getSignatureBytes().toByteArray());
	}

}
