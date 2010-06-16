package edu.rice.batchsig.bench;

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


import com.google.protobuf.ByteString;

import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.SignatureAlgorithm;

public class PublicKeyPrims implements SignaturePrimitives {
	
	final byte signer_id_bytes[];
	final ByteString signer_id;
	final Signature signer, verifier;
	final SignatureAlgorithm sigalgo;
	
	public PublicKeyPrims(String signer_id_string, String algo, int size) throws NoSuchAlgorithmException, InvalidKeyException  {
		this.signer_id_bytes = signer_id_string.getBytes();
		this.signer_id = ByteString.copyFrom(signer_id_bytes);
		
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

		if (algo.toLowerCase().equals("rsa")) {
			sigalgo = SignatureAlgorithm.SHA1_RSA;
		} else if (algo.toLowerCase().equals("dsa")) {
			sigalgo = SignatureAlgorithm.SHA1_DSA;
		} else {
			throw new Error("Unknown signature algorithm");
		}
	
	}

	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		try {
		signer.update(data);
		out.setSignatureBytes(ByteString.copyFrom(signer.sign()));
		out.setSignerId(signer_id);
		} catch (SignatureException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		if (sig.getSignatureAlgorithm() != sigalgo) {
			System.out.println("Info: Mismatched signature algorithms");
			return false;
		}
		if (!Arrays.equals(signer_id_bytes, sig.getSignerId().toByteArray())) {
			System.out.println("Info: Mismatched signerids");
			return false;
		}
		try {
			return verifier.verify(sig.getSignatureBytes().toByteArray());
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		}
	}
}
