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

import java.io.FileInputStream;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.IncomingMessageStreamFromFile;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;

/** Given a logfile of 'messages' that were signed, verify them 'in real time', using the included timestamp. Used to benchmark verification. */


public class ReplaySavedMessagesRealtimeThread extends MessageGeneratorThreadBase {
	final private IncomingMessageStreamFromFile input;

	private final VerifyHisttreeLazilyQueue lazyqueue;
	
	/** Add new messages to the queue at the requested. 
	 * 
	 * @param maxsize Messages per second.
	 * */
	public ReplaySavedMessagesRealtimeThread(VerifyHisttreeLazilyQueue verifyqueue, FileInputStream fileinput, int maxsize) {
		super(verifyqueue,maxsize);
		if (fileinput == null)
			throw new Error();
		this.input = new IncomingMessageStreamFromFile(fileinput);
		lazyqueue = verifyqueue;
	}

	/** Setup the replay trace, preloading the bias and the keys. */
	public void setup(MultiplexedPublicKeyPrims prims) {
		// First pass: Preload all of the verification keys and get the timestamp bias.
		IncomingMessage im;
		while ((im = input.nextOnePass()) != null) {
			prims.load(im.getSignatureBlob()); // Fetch the signature blob.
		}
		input.resetStream();
	}
	
	@Override
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.
		
		while (!finished.get()) {
			IncomingMessage msg = input.nextOnePass();
			// We're at the end.
			if (msg == null) {
				System.out.println("EOF");
				break;
			}
				
			// STEP 1: Delay until the inject time equals this time.
			
			// Offset in ms from the first message in the trace.
			long msgOffsetTime = msg.getVirtualClock();
			// What time should we insert.
			long injectTime = msgOffsetTime + initTime;
			long now = System.currentTimeMillis();

			if (injectTime > now) {
				// Running ahead. Lets sleep for a little bit. 
				try {
					Thread.sleep((injectTime-now));
				} catch (InterruptedException e) {
				}
			}
			if (msg.getData() != null) {
				//System.out.println("Bad message "+msg.getVirtualClock() + "data" + msg.getData().length);
				//if (msg.getSignatureBlob() == null) {System.out.println("Interesting...."); Thread.dumpStack();}
				//if (msg.getAuthor() == null) {System.out.println("Interesting...."); Thread.dumpStack();}
				lazyqueue.add(msg);
			}
			// STEP 3: Play the forced back.
			if (msg.end_buffering != null)
				for (Integer i: msg.end_buffering) {
					lazyqueue.forceUser(i);
			checkQueueOverflow();
			}
			//System.out.println("Iterating replay loop");
		}
		queue.finish();
	}
}
