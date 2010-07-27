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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.VerifyQueue;

// java -cp lib/bb.jar:lib/bcprov.jar:lib/jsci-core.jar:lib/mt-13.jar:bin/:/usr/share/java/protobuf.jar  edu.rice.batchsig.bench.BenchSigner simple sha1withrsa 1024


public class BenchVerifier {
	boolean isBatch, isBig;
	QueueBase queue;
	FileOutputStream file;
	CodedOutputStream output;
	SignaturePrimitives prims;
	Tracker tracker = new Tracker();
	String ciphertype,queuetype;
	

	BenchVerifier setupCipher(String type, int bits) throws InvalidKeyException, NoSuchAlgorithmException {
		// Must set the prims first, used with the other.
		prims = PublicKeyPrims.make("Bench",type,bits);
		ciphertype = type;
		return this;
	}
	
	void finish() throws IOException {
	}
		
	BenchVerifier setupQueue() {
		this.queue = new VerifyQueue(prims);
		return this;
	}	

	
	public void initialization(FileInputStream input, int makeRate, int signRate, int sleepTime) throws InterruptedException {
		ReplayMessagesThread makeThread = new ReplayMessagesThread(queue,input, tracker, makeRate);
		SignMessageThread signThread = new SignMessageThread(queue, 0);
		makeThread.start();
		signThread.start();
		Thread.sleep(sleepTime);
		makeThread.shutdown(); makeThread.join();
		signThread.shutdown(); signThread.join();
		}
	
		
	public void hotSpot(FileInputStream input) throws InterruptedException {
		if (isBatch) {
			this.initialization(input,100,1,500);
			this.initialization(input,1000,1,1000);
			this.initialization(input,10000,1,5000);
			if (isBig) 
				this.initialization(input,10000, 1, BIGTIME);
		} else {
			this.initialization(input,10,1,100);
			if (ciphertype.endsWith("rsa")) {
				this.initialization(input,50,1,1000);
				this.initialization(input,50,1,5000);
				if (isBig)
					this.initialization(input,50, 1, BIGTIME);
			} else {
				this.initialization(input,50,1,1000);
				this.initialization(input,300,1,5000);
				if (isBig)
					this.initialization(input,300, 1, BIGTIME);
			}
		}
	}

	public void doBenchOne() throws InterruptedException {
		tracker.enable();
		int time = isBig ? BIGTIME : 5000;
		if (isBatch) {
			initialization(null,40000,1,time);
		} else {
			initialization(null,300,1,time);
		}
	}

	static final int BIGTIME = 120000;
	static final int NORMALTIME = 5000;

	public void doBenchMany(FileInputStream input) throws InterruptedException {
		int time = isBig ? NORMALTIME : 5000;
		int rate,incr;
		if (isBatch) {
			rate = 10000;
			incr = 1000;
		} else {
			if (ciphertype.endsWith("rsa")) {
				rate = 2000;
				incr = 100;
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
			initialization(input,rate,1,time);
			tracker.print(String.format("%05d",rate));
			rate += incr;
		} while(tracker.isAborting() != true);
	}
	
	public static void main(String args[]) throws FileNotFoundException {
		Security.addProvider(new BouncyCastleProvider());
		try {
			BenchVerifier bench = new BenchVerifier();
			bench.setupCipher(args[0],Integer.parseInt(args[1]));
			bench.setupQueue();

			//PublicKeyPrims verifyprims = PublicKeyPrims.createNew("Bench",args[1],Integer.parseInt(args[2]));
			//VerifyQueue verifyqueue = new VerifyQueue(verifyprims);

				FileInputStream fileinput = new FileInputStream(args[2]);
				bench.hotSpot(fileinput);
				bench.doBenchMany(fileinput);

			System.exit(0);
			
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
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


