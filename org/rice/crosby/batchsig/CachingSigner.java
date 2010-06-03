package org.rice.crosby.batchsig;

/** Wraps a signer with one that caches signature verifications so that the same signature and data need only be verified once */
public class CachingSigner implements SignaturePrimitives {
	final private SignaturePrimitives orig;

	CachingSigner(SignaturePrimitives orig) {
		this.orig = orig;
	}
		
	@Override
	public byte[] sign(byte[] data) {
		return orig.sign(data);
	}

	@Override
	public boolean verify(byte[] data, byte[] sig) {
		// TODO Implement the cache.
		return orig.verify(data,sig);
	}

}
