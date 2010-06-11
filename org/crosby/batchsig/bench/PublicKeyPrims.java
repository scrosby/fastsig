package org.crosby.batchsig.bench;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;

import org.rice.crosby.batchsig.SignaturePrimitives;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

import com.google.protobuf.ByteString;

public class PublicKeyPrims implements SignaturePrimitives {
	static final byte default_signerid_bytes[] = "Test Public Signature".getBytes();
	static final ByteString default_signerid = ByteString.copyFrom(default_signerid_bytes);
	final Signature signer, verifier;

	public PublicKeyPrims(String algo, int size) throws NoSuchAlgorithmException, InvalidKeyException  {
		System.out.println("START: "+(new Date()).toString());
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo);
		kpg.initialize(size);
		KeyPair kp = kpg.genKeyPair();
		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();

		signer = Signature.getInstance("SHA1with"+algo);
		signer.initSign(privateKey);
		verifier = Signature.getInstance("SHA1with"+algo);
		verifier.initVerify(publicKey);
	}

	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		try {
		signer.update(data);
		out.setSignatureBytes(ByteString.copyFrom(signer.sign()));
		out.setSignerId(default_signerid);
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		// TODO Auto-generated method stub
		if (!Arrays.equals(default_signerid_bytes, sig.getSignerId().toByteArray())) {
			System.out.println("Mismatched signerids");
			return false;
		}
		try {
			return verifier.verify(sig.getSignatureBytes().toByteArray());
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
