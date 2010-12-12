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

package edu.rice.batchsig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


import com.google.protobuf.ByteString;

import edu.rice.historytree.generated.Serialization.SigTreeType;
import edu.rice.historytree.generated.Serialization.SignatureType;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigMessage;

/** Sign a set of messages by signing them one at a time.
 * 
 */
public class SimpleQueue extends QueueBase<OMessage> implements SuspendableProcessQueue<OMessage> {
	public SimpleQueue(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	public void process() {
		long now = System.currentTimeMillis();
		ArrayList<OMessage> oldqueue = atomicGetQueue();
		//System.out.println(String.format("Processing batch of %d messages at time %d",oldqueue.size(),now-initTime));
		if (oldqueue.size() == 0)
			return;
		for (OMessage m : oldqueue) {
			TreeSigBlob.Builder sigblob = TreeSigBlob.newBuilder();
			sigblob.setSignatureType(SignatureType.SINGLE_MESSAGE);

			// What is signed.
			TreeSigMessage.Builder msgbuilder = 
				TreeSigMessage.newBuilder()
				.setTreetype(SigTreeType.MESSAGE)
				.setRoothash(ByteString.copyFrom(hash(m.getData())));

			// Sign it, storing the signature in the blob.
			signer.sign(msgbuilder.build().toByteArray(),sigblob);

			m.signatureResult(sigblob.build());
		}
	}

	/** Helper function for hashing a byte array. */
	public static byte[] hash(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	}
