package cryptobench;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;



public class SHA1BenchOrig {

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
		//printProviders();
		String provider = null;
		if (args.length >= 1)
			provider=args[0];
		
		SHA1Bench bench;
		try {
			bench = new SHA1Bench(provider);
			bench.bench();		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	MessageDigest sha;
    Mac hm = null;


	void bench() throws IllegalArgumentException, IllegalStateException, Exception {
		String alg = "SHA256";
		//byte keyBytes[] = {0,2,4,6,8};
		byte src[] = new byte[64];
		for (int i = 0 ; i < src.length ; i++)
			src[i] = (byte)(1001145851*i);
	      hm = Mac.getInstance(alg);
	      //Key k1 = new SecretKeySpec(keyBytes, 0, keyBytes.length, alg);
	      //hm.init(k1);
		//sha = MessageDigest.getInstance("HMACSHA256");		
		System.out.println("SHA256Bench: "+(new Benchmark(hashOne(src))));
	}

	Signature signer, verifier;
	public SHA1BenchOrig(String provider) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
		System.out.println("START: "+(new Date()).toString());
	}
	
	public Callable<byte[]> hashOne (final byte src[]) {
		return new Callable<byte[]>() {
			public byte [] call() {
				    src[0] += 3;
				    if (src[0] == 0) src[1] += 5;
				    hm.update(src);
				    byte[] msgDigest = hm.doFinal();
					return msgDigest;
		};
	};
	}
}
