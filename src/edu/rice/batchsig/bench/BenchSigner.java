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

package edu.rice.batchsig.bench;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.HistoryQueue;
import edu.rice.batchsig.MerkleQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.SimpleQueue;
import edu.rice.historytree.generated.Serialization.MessageData;

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
