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
import java.security.Security;
import java.util.Random;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.HistoryQueue;
import edu.rice.batchsig.MerkleQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.SimpleQueue;
import edu.rice.historytree.generated.Serialization.MessageData;

// java -cp lib/bb.jar:lib/bcprov.jar:lib/jsci-core.jar:lib/mt-13.jar:bin/:/usr/share/java/protobuf.jar  edu.rice.batchsig.bench.BenchSigner simple sha1withrsa 1024


public class BenchSigner {
	boolean isBatch, isBig;
	QueueBase queue;
	FileOutputStream file;
	CodedOutputStream output;
	SignaturePrimitives prims;
	LatencyTracker tracker = new LatencyTracker();
	String ciphertype;
	
	BenchSigner(String args[]) throws FileNotFoundException, InvalidKeyException, NumberFormatException, NoSuchAlgorithmException {
		//file = new FileOutputStream("signedmessages");
		//output = CodedOutputStream.newInstance(file);
		// Must set the prims first, used with the other.
		//prims = new PublicKeyPrims("Bench",args[1],Integer.parseInt(args[2]));
		//setupQueue(prims,args[0]);
	}

	BenchSigner setupCipher(String type, int bits) throws InvalidKeyException, NoSuchAlgorithmException {
		// Must set the prims first, used with the other.
		prims = new PublicKeyPrims("Bench",type,bits);
		ciphertype = type;
		return this;
	}
	
	void finish() throws IOException {
		if (file != null) {
			output.flush();
			file.close();
		}
	}
		
	BenchSigner setupQueue(String type) {
		if (type.endsWith("history")) {
			isBatch = true;
			queue=new HistoryQueue(prims);
		} else if (type.endsWith("merkle")) {
			isBatch = true;
			queue=new MerkleQueue(prims);
		} else if (type.endsWith("simple")) {
			isBatch = false;
			queue=new SimpleQueue(prims);
		} else 
			throw new IllegalArgumentException(String.format("Type %s not understood",type));

		if (type.startsWith("big"))
			isBig = true;
		else
			isBig = false;
			
		return this;
	}	

	
	public void initialization(int makeRate, int signRate, int sleepTime) throws InterruptedException {
		MakeMessagesThread makeThread = new MakeMessagesThread(queue, tracker, makeRate);
		SignMessageThread signThread = new SignMessageThread(queue, signRate);
		makeThread.start();
		signThread.start();
		Thread.sleep(sleepTime);
		makeThread.shutdown(); makeThread.join();
		signThread.shutdown(); signThread.join();
		}
	
	
	public void hotSpotInitialization() {
		String args[]; 
		String foo0[] = {"simple", "merkle", "history"};
		String foo1[] = {"sha1withrsa", "sha256withrsa","sha256withdsa","sha1withdsa"};
		int foo2[] = {1024,2048};
	}
	
	public void hotSpotCiphers() throws InvalidKeyException, NumberFormatException, FileNotFoundException, NoSuchAlgorithmException, InterruptedException {
		/*initialization(new BenchSigner( new String[]{"simple","sha1withrsa","2048"} ),100,1,500);
		initialization(new BenchSigner( new String[]{"simple","sha1withdsa","2048"} ),100,1,500);

		initialization(new BenchSigner( new String[]{"simple","sha1withrsa","2048"} ),100,1,500);
		initialization(new BenchSigner( new String[]{"simple","sha1withdsa","2048"} ),100,1,500);

		initialization(new BenchSigner( new String[]{"simple","sha1withrsa","2048"} ),100,1,500);
	*/
	}
	
	public void hotSpot() throws InterruptedException {
		if (isBatch) {
			this.initialization(100,1,500);
			this.initialization(1000,1,1000);
			this.initialization(10000,1,5000);
			if (isBig) 
				this.initialization(10000, 1, BIGTIME);
		} else {
			this.initialization(10,1,100);
			if (ciphertype.endsWith("rsa")) {
				this.initialization(50,1,1000);
				this.initialization(50,1,5000);
				if (isBig)
					this.initialization(50, 1, BIGTIME);
			} else {
				this.initialization(50,1,1000);
				this.initialization(300,1,5000);
				if (isBig)
					this.initialization(300, 1, BIGTIME);
			}
		}
	}

	public void doBenchOne() throws InterruptedException {
		tracker.enable();
		int time = isBig ? BIGTIME : 5000;
		if (isBatch) {
			initialization(40000,1,time);
		} else {
			initialization(300,1,time);
		}
	}

	static final int BIGTIME = 120000;
	
	public void doBenchMany() throws InterruptedException {
		int time = isBig ? BIGTIME : 5000;
		int rate,incr;
		if (isBatch) {
			rate = 10000;
			incr = 1000;
		} else {
			if (ciphertype.endsWith("rsa")) {
				rate = 10;
				incr = 1;
			} else {
				rate = 300;
				incr = 5;
			}
		}
		do {
			System.err.format("**** RUNNING  rate=%d ****\n",rate);
			System.err.flush();
			tracker.reset();
			tracker.enable();
			initialization(rate,1,time);
			tracker.print();
			rate += incr;
		} while(tracker.isAborting() != true);
	}
	
	public static void main(String args[]) {
		Security.addProvider(new BouncyCastleProvider());
		try {
			BenchSigner bench = new BenchSigner(args);
			bench.setupCipher(args[1],Integer.parseInt(args[2]));
			bench.setupQueue(args[0]);
			// Pre-load the hotspot.
			bench.hotSpot();
			bench.doBenchMany();
			System.exit(0);
			
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


// 781 271 3186
   //   This is Suzane kurnst cal version....    781 271 3186


