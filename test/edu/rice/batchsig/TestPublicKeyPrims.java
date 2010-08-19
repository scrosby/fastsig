package edu.rice.batchsig;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

import org.junit.Test;

import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.bench.PublicKeyPrims;
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

import junit.framework.TestCase;


public class TestPublicKeyPrims extends TestCase {
	byte in1[] = {0,1,2,3,4,-1,6,7};
	byte in2[] = {0,1,2,3,4,-1,6,9};

	SignaturePrimitives prims;
	
	public TestPublicKeyPrims()  {
		try {
			prims = PublicKeyPrims.make("NULL", "sha1withdsa", 512, null);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	TreeSigBlob sign(byte in[]) {
		TreeSigBlob.Builder sigblob = TreeSigBlob.newBuilder();
		prims.sign(in,sigblob);
		sigblob.setSignatureType(SignatureType.SINGLE_MESSAGE);
		return sigblob.build();
	}
	


	@Test
	public void testSignaturesVerify() {
		TreeSigBlob out1 = sign(in1);
		TreeSigBlob out2 = sign(in2);

		System.out.println(in1.toString());
		System.out.println(out1.toString());

		assertTrue(prims.verify(in1,out1));
		assertTrue(prims.verify(in2,out2));
		assertFalse(prims.verify(in2,out1));
		assertFalse(prims.verify(in1,out2));
	}

}


