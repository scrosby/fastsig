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
import java.util.HashMap;

import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.bench.PublicKeyPrims;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.Builder;

class MultiplexedPublicKeyPrims implements SignaturePrimitives {
	final String algo;
	final int size;
	final HashMap<Object,SignaturePrimitives> map = new HashMap<Object,SignaturePrimitives>();

	MultiplexedPublicKeyPrims(String algo, int size) {
		this.algo = algo;
		this.size = size;
	}
	
	
	SignaturePrimitives load(TreeSigBlob.Builder msg) throws InvalidKeyException, NoSuchAlgorithmException {
		String signer = msg.getSignerId().toStringUtf8();
		if (!map.containsKey(msg.getSignerId().toStringUtf8()))
			map.put(signer,PublicKeyPrims.make(signer, algo, size));
		return map.get(signer);
	}

	SignaturePrimitives load(TreeSigBlob msg) throws InvalidKeyException, NoSuchAlgorithmException {
		String signer = msg.getSignerId().toStringUtf8();
		if (!map.containsKey(msg.getSignerId().toStringUtf8()))
			map.put(signer,PublicKeyPrims.make(signer, algo, size));
		return map.get(signer);
	}
	
	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		try {
			load(out).sign(data, out);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		try {
			return load(sig).verify(data, sig);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}


}