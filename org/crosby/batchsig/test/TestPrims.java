package org.crosby.batchsig.test;

import java.util.Arrays;

import org.junit.Test;
import org.rice.crosby.batchsig.SignaturePrimitives;
import org.rice.crosby.historytree.generated.Serialization.SignatureType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

import junit.framework.TestCase;


public class TestPrims extends TestCase {
	byte in1[] = {0,1,2,3,4,-1,6,7};
	byte in2[] = {0,1,2,3,4,-1,6,9};

	SignaturePrimitives prims = new DigestPrimitive();

	byte[] simpleSign(byte in[]) {
		TreeSigBlob.Builder out = TreeSigBlob.newBuilder();
		prims.sign(in,out);
		return out.getSignatureBytes().toByteArray();
	}

	TreeSigBlob sign(byte in[]) {
		TreeSigBlob.Builder sigblob = TreeSigBlob.newBuilder();
		prims.sign(in,sigblob);
		sigblob.setSignatureType(SignatureType.SINGLE_MESSAGE);
		return sigblob.build();
	}
	
	@Test
	public void testDigestPrimitive() {
		byte out1[] = simpleSign(in1);
		byte out2[] = simpleSign(in2);
		
		assertFalse(Arrays.equals(out1,out2));
	}

	@Test
	public void testSignaturesVerify() {
		TreeSigBlob out1 = sign(in1);
		TreeSigBlob out2 = sign(in2);
		
		assertTrue(prims.verify(in1,out1));
		assertTrue(prims.verify(in2,out2));
		assertFalse(prims.verify(in2,out1));
		assertFalse(prims.verify(in1,out2));
	}

}


