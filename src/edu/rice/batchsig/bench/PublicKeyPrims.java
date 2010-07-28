/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.batchsig.bench;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.SignatureAlgorithm;

public class PublicKeyPrims implements SignaturePrimitives {
	
	byte signer_id_bytes[];
	ByteString signer_id;
	Signature signer, verifier;
	SignatureAlgorithm sigalgo;
	KeyPair kp;
	
	private PublicKeyPrims() {
	}
	
	public static PublicKeyPrims make(String signer_id_string, String algo, int size) throws NoSuchAlgorithmException, InvalidKeyException  {
		PublicKeyPrims out = new PublicKeyPrims();
		
		out.signer_id_bytes = signer_id_string.getBytes();
		out.signer_id = ByteString.copyFrom(out.signer_id_bytes);

		// If we already have it....
		if (out.load(signer_id_string,algo,size))
			return out;

		System.err.println("Making new keypair");
		
		// Otherwise, generate it.
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo.substring(algo.length()-3));
		kpg.initialize(size);
		out.initialize(kpg.genKeyPair(),algo, size);
		out.save(makeFilename(signer_id_string,algo,size));
		return out;
	}
		
	static String makeFilename(String signer_id_string, String algo, int size) {
		return String.format("KEY.%s-%s-%d.key",signer_id_string,algo,size);
	}
	
	void initialize(KeyPair kp, String algo, int size) throws NoSuchAlgorithmException, InvalidKeyException {
		this.kp = kp;
		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();
		
		signer = Signature.getInstance(algo);
		signer.initSign(privateKey);
		verifier = Signature.getInstance(algo);
		verifier.initVerify(publicKey);

		if (algo.toLowerCase().equals("sha1withrsa")) {
			sigalgo = SignatureAlgorithm.SHA1_RSA;
		} else if (algo.toLowerCase().equals("sha1withdsa")) {
			sigalgo = SignatureAlgorithm.SHA1_DSA;
		} else if (algo.toLowerCase().equals("sha256withrsa")) {
			sigalgo = SignatureAlgorithm.SHA1_RSA;
		} else if (algo.toLowerCase().equals("sha256withdsa")) {
			sigalgo = SignatureAlgorithm.SHA1_DSA;
		} else {
			throw new Error("Unknown signature algorithm");
		}
	}
	
	public void save(String filename) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
			out.writeObject(kp);
			out.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public boolean load(String signer_id_string, String algo, int size) {
		try {
			ObjectInputStream input = new ObjectInputStream(new FileInputStream(makeFilename(signer_id_string,algo,size)));
			initialize((KeyPair)input.readObject(),algo,size);
			input.close();
			return true;
		} catch (IOException e) {
			// If it fails, just generate it.
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		try {
		Tracker.singleton.signcount++;
		signer.update(data);
		out.setSignatureAlgorithm(sigalgo);
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
			Tracker.singleton.verifycount++;
			verifier.update(data);
			return verifier.verify(sig.getSignatureBytes().toByteArray());
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		}
	}
}
