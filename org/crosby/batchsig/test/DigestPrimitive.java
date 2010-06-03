package org.crosby.batchsig.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.rice.crosby.batchsig.SignaturePrimitives;
import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob.Builder;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob.SignatureAlgorithm;

import com.google.protobuf.ByteString;
import com.sun.org.apache.xml.internal.security.utils.Base64;


/** 'Sign' a string by computing a digest over it. 
 * 
 * Use this for testing. 
 * For testing.
 * 
 * @author crosby
 *
 */
public class DigestPrimitive implements SignaturePrimitives {

	static public byte[] hash(byte[] data) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			md.update(data);
			return Base64.encode(md.digest()).getBytes();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		out.setSignatureAlgorithm(SignatureAlgorithm.TEST_DIGEST);

		//System.out.println("Signing '"+new String(data)+"' with "+new String(hash(data)));
		
		out.setSignatureBytes(ByteString.copyFrom(hash(data)));
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		//System.out.println("Validating '"+new String(data)+"' : "+new String(hash(data)) + "  == " + new String(sig.getSignatureBytes().toByteArray()));
		if (sig.getSignatureAlgorithm() == SignatureAlgorithm.TEST_DIGEST)
			return Arrays.equals(hash(data),sig.getSignatureBytes().toByteArray());
		System.out.println("A non-'test' signature sent under test");
		return false;
	}

}
