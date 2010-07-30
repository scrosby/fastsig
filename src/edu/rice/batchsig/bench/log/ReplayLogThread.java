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

package edu.rice.batchsig.bench.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.IncomingMessageStream;
import edu.rice.batchsig.bench.ShutdownableThread;
import edu.rice.batchsig.bench.Tracker;
import edu.rice.historytree.generated.Serialization.MessageData;

/** Given a logfile of 'messages' to be signed, play them. Each message has an arrival timestamp. */

public class ReplayLogThread extends Thread implements ShutdownableThread {
	final private int rate;
	final private QueueBase verifyqueue;
	final private AtomicBoolean finished = new AtomicBoolean(false);
	final private IncomingMessageStream input;
	long bias = -1; // Difference betweeen 'real' clock and virtual clock.

	/** Add new messages to the queue at the requested. 
	 * 
	 * @param rate Messages per second.
	 * */
	ReplayLogThread(QueueBase verifyqueue, FileInputStream fileinput, int rate) {
		if (fileinput == null)
			throw new Error();
		if (verifyqueue == null)
			throw new Error();
		this.verifyqueue = verifyqueue;
		this.rate = rate;
		this.input = new IncomingMessageStream(fileinput);
	}

	/** Setup the replay log, preloading the bias and the keys. */
	void setup(MultiplexedPublicKeyPrims prims) {
		long bias = -1; // Difference betweeen 'real' clock and virtual clock.

		// First pass: Preload all of the verification keys and get the timestamp bias.
		IncomingMessage im;
		while ((im = input.nextOnePass()) != null) {
			if (bias == -1)
				bias = im.getVirtualClock();
			prims.load(im.getSignatureBlob()); // Fetch the signature blob.
		}
		input.resetStream();
	}
	
	
	/* (non-Javadoc)
	 * @see edu.rice.batchsig.bench.ShutdownableThread#shutdown()
	 */
	public void shutdown() {
		finished.set(true);
	}
	
	
	@Override
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.
		long insertedNum = 0;
		while (!finished.get()) {
			IncomingMessage msg = input.next();
			
			// Offset in ms from the first message in the log.
			long msgOffsetTime = msg.getVirtualClock()-bias;
			// What time should we insert.
			long injectTime = msgOffsetTime + initTime;
			long now = System.currentTimeMillis();

			if (injectTime > now) {
				// Running ahead. Lets sleep for a little bit. 
				try {
					Thread.sleep(injectTime-now);
				} catch (InterruptedException e) {
				}
			} else {
				verifyqueue.add(input.nextOnePass());
				checkQueueOverflow();
			}
		}
		verifyqueue.finish();
	}

	static long lastErr = 0;
	static long skip = 0;
	
	void checkQueueOverflow() {
		if (verifyqueue.peekSize() > rate) {
			skip++;
			if (System.currentTimeMillis() > lastErr + 1000) {
				System.err.format("Queue overfull(%d)\n",skip);
				if (skip > 1)
					Tracker.singleton.markAbort();
				skip=0; lastErr = System.currentTimeMillis();
			}
		}
	}
}
