package org.rice.crosby.batchsig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.rice.crosby.historytree.generated.Serialization.SigTreeType;
import org.rice.crosby.historytree.generated.Serialization.SignatureType;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;
import org.rice.crosby.historytree.generated.Serialization.TreeSigMessage;

import com.google.protobuf.ByteString;

/** Simple queue that signs every message */
public class SimpleQueue extends QueueBase {
	private SignaturePrimitives signer;

	public SimpleQueue(SignaturePrimitives signer) {
		super();
		this.signer = signer;
	}


	public void process() {
		ArrayList<Message> oldqueue = atomicGetQueue();
		if (oldqueue.size() == 0)
			return;
		for (Message m : oldqueue) {
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

	static public byte[] hash(byte[] data) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			md.update(data);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	}
