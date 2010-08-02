package cryptobench;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;

import bb.util.Benchmark;
import java.security.Security;
import java.security.Provider.Service;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;



public class RSABench {

	static void printProviders() {
		for (Provider i : Arrays.asList(Security.getProviders())) {
			System.out.println(String.format("Provider(%s):%s",i.getName(),i.getInfo()));
			for (Service j : i.getServices())
				System.out.println("    Algo:"+j.getAlgorithm());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		printProviders();
		String name = args[0];
		String provider = null;
		int size = Integer.parseInt(args[1]);
		if (args.length > 2)
			provider=args[2];
		
		RSABench bench;
		try {
			bench = new RSABench(name,size,provider);
			bench.bench();		
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
	public RSABench(String algo, int size, String provider) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
		System.out.println("START: "+(new Date()).toString());
		KeyPairGenerator kpg;
		if (provider != null)
			kpg = KeyPairGenerator.getInstance(algo,provider);
		else
			kpg = KeyPairGenerator.getInstance(algo);
		
		if (size > 0) 
			kpg.initialize(size, new SecureRandom());
		else 
			switch (size) {
			case -192: 
				kpg.initialize(ECNamedCurveTable.getParameterSpec("prime192v1"),new SecureRandom());
				break;
			case -256:
				kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"),new SecureRandom());
				break;
			case -224:
				kpg.initialize(ECNamedCurveTable.getParameterSpec("secp224r1"),new SecureRandom());
				break;
			default:
				//throw new Error("XXX");
			}
		KeyPair kp = kpg.genKeyPair();
		publicKey = kp.getPublic();
		privateKey = kp.getPrivate();
		
		String algoname;
		if (size < 0) {
			algoname = "SHA256with"+algo;
		} else {
			algoname = "SHA1with"+algo;
			if (algo.toLowerCase().equals("ec")) 
				algoname = "SHA256withECDSA";
		}		
		if (provider != null) {
			System.err.println("Building with: " + algoname + "  " + provider);
			signer = Signature.getInstance(algoname,provider);
			verifier = Signature.getInstance(algoname,provider);
		} else {
			System.err.println("Building with: " + algoname + "  <UNKNOWN>");
			signer = Signature.getInstance(algoname);
			verifier = Signature.getInstance(algoname);
		}
		signer.initSign(privateKey);
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
