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
 * @author Scott A. Crosby crosby@cs.rice.edu
 *
 */

package edu.rice.batchsig.bench.log;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.bench.PublicKeyPrims;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.Builder;

public class MultiplexedPublicKeyPrims implements SignaturePrimitives {
	final String algo;
	final int size;
	final HashMap<Object,SignaturePrimitives> map = new HashMap<Object,SignaturePrimitives>();
	final String provider;
	
	MultiplexedPublicKeyPrims(String algo, int size, String provider) {
		this.algo = algo;
		this.size = size;
		this.provider = provider;
	}
	
	public SignaturePrimitives load(String signer) {
		if (signer.equals(""))
			throw new Error("Illegal empty signer_id");
		try {
			if (!map.containsKey(signer))
				map.put(signer,PublicKeyPrims.make(signer, algo, size, provider));
			return map.get(signer);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public SignaturePrimitives load(TreeSigBlob.Builder msg) {
		return load(msg.getSignerId().toStringUtf8());
	}

	public SignaturePrimitives load(TreeSigBlob msg) {
		return load(msg.getSignerId().toStringUtf8());
	}
	
	@Override
	/** Assume that the builder has already been marked with the signer's ID */
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		throw new Error("Shouldn't be invoked?");
		//load(out).sign(data, out);
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		return load(sig).verify(data, sig);
	}

	/** Make a given key
	 * 
	 * @param algo Algorithm to use (e.g. "sha1withdsa")
	 * @param size Number of bits in the key
	 * @param provider Which provider (May be null)
	 * @return A primitive object.
	 */
	public static MultiplexedPublicKeyPrims make(String algo, int size, String provider) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException  {
		return new MultiplexedPublicKeyPrims(algo,size,provider);
	}		
		
}