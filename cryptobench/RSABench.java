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
		}
		
	}
	void bench() throws SignatureException {
		byte src[] = {0,2,4,6,8};
		byte sig[];

		int i;

		Date start,stop;
		
		start = new Date(); i=0;
		System.out.println("SIG: "+start.toString());

		while (true) {
			i++;
			sig =signOne(src);
			if (i%10 == 0) {
				stop = new Date();
				if ((stop.getTime()-start.getTime())/1000 > 10) 
					break;
			}
		}
		System.out.format("SIG: %s  %d   %d/sec\n",stop.toString(),i,1000*i/(stop.getTime()-start.getTime()));

		
		start = new Date(); i=0;
		stop = new Date();
		System.out.println("VFY: "+start.toString());

		while (true) {
			i++;
			verifyOne(src,sig);
			if (i%10 == 0) {
				stop = new Date();
				if ((stop.getTime()-start.getTime())/1000 > 10) 
					break;
			}
		}
		System.out.format("VFH: %s  %d     %d/sec\n",stop.toString(),i,1000*i/(stop.getTime()-start.getTime()));

	
	
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
	
	public byte[] signOne (byte src[]) throws SignatureException {
		signer.update(src);
		return signer.sign();
	}
	
	public void verifyOne (byte src[], byte signature[]) throws  SignatureException {
		verifier.update(src);
		verifier.verify(signature);
	}
		
	PublicKey publicKey;
	PrivateKey privateKey;
}
