package cryptobench;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Date;
import java.util.concurrent.Callable;

import bb.util.Benchmark;


public class RSABench {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String name = args[0];
		int size = Integer.parseInt(args[1]);
		
		RSABench bench;
		try {
			bench = new RSABench(name,size);
			bench.bench();		
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
		
	void bench() throws IllegalArgumentException, IllegalStateException, Exception {
		byte src[] = {0,2,4,6,8};

		byte[] sig=signOne(src).call();
		
		System.out.println("SignBench: "+(new Benchmark(signOne(src))));
		System.out.println("VerifyBench: "+(new Benchmark(verifyOne(src,sig))));
	}

	

	Signature signer, verifier;
	public RSABench(String algo, int size) throws NoSuchAlgorithmException, InvalidKeyException {
		System.out.println("START: "+(new Date()).toString());
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo);
		kpg.initialize(size);
		KeyPair kp = kpg.genKeyPair();
		publicKey = kp.getPublic();
		privateKey = kp.getPrivate();

		signer = Signature.getInstance("SHA1with"+algo);
		signer.initSign(privateKey);
		verifier = Signature.getInstance("SHA1with"+algo);
		verifier.initVerify(publicKey);
	}
	
	public Callable<byte[]> signOne (final byte src[]) {
		return new Callable<byte[]>() {
			public byte [] call() {
				try {
					signer.update(src);
					return signer.sign();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
		};
	}
	
	public Callable<Void> verifyOne (final byte src[], final byte signature[]) {
		return new Callable<Void>() {
			public Void call() {
				try {
				verifier.update(src);
				verifier.verify(signature);
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
				
			}
		};
	}
		
	PublicKey publicKey;
	PrivateKey privateKey;
}
