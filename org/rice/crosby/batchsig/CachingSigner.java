package org.rice.crosby.batchsig;

import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob.Builder;

/** Wraps a signer with one that caches signature verifications so that the same signature and data need only be verified once */
public class CachingSigner implements SignaturePrimitives {
	final private SignaturePrimitives orig;

	CachingSigner(SignaturePrimitives orig) {
		this.orig = orig;
	}

	@Override
	public void sign(byte[] data, Builder out) {
		orig.sign(data,out);
		
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		// TODO Implement the cache.
		return orig.verify(data,sig);
	}

}
