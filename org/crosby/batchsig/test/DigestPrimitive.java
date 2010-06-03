package org.crosby.batchsig.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.rice.crosby.batchsig.SignaturePrimitives;


/** 'Sign' a string by computing a digest over it. 
 * 
 * Use this for testing. 
 * For testing.
 * 
 * @author crosby
 *
 */
public class DigestPrimitive implements SignaturePrimitives {

	@Override
	public byte[] sign(byte[] data) {
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
	public boolean verify(byte[] data, byte[] sig) {
		byte digest[] = sign(data);
		return Arrays.equals(digest,sig);
	}

}
