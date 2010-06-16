package org.crosby.batchsig.bench;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.rice.crosby.batchsig.HistoryQueue;
import org.rice.crosby.batchsig.MerkleQueue;
import org.rice.crosby.batchsig.QueueBase;
import org.rice.crosby.batchsig.SignaturePrimitives;
import org.rice.crosby.batchsig.SimpleQueue;
import org.rice.crosby.historytree.generated.Serialization.MessageData;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

public class BenchSigner {
	static int seqno = 0;
	static Random rand = new Random();
	static Object objs[] = new Object[20];
	static {
		for (int i = 0 ; i < objs.length ; i++)
			objs[i] = new Object();
	}

	SignaturePrimitives prims;
	QueueBase queue;
	FileOutputStream file;
	CodedOutputStream output;
	
	BenchSigner() throws FileNotFoundException {
		file = new FileOutputStream("signedmessages");
		output = CodedOutputStream.newInstance(file);
	}
	
	void finish() throws IOException {
		output.flush();
		file.close();
		
	}
		
	void setupQueue(String type) {
		if (type.equals("history"))
			queue=new HistoryQueue(prims);
		else if (type.equals("merkle"))
			queue=new MerkleQueue(prims);
		else if (type.equals("simple"))
			queue=new SimpleQueue(prims);
		else 
			throw new IllegalArgumentException(String.format("Type %d not understood",type));
	}
	
	void setupPrims(String sigtype, int bits) throws InvalidKeyException, NoSuchAlgorithmException {
		prims = new PublicKeyPrims("Bench",sigtype,bits);
	}		

	void makeMessageData(int seqno) {
		queue.add(new OutgoingMessage(output,String.format("Msg:%d",seqno++).getBytes(),pickSource()));
		}

	
	Object pickSource() {
		return objs[rand.nextInt(objs.length)];
	}
	
}
