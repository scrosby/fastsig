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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.IncomingMessageStreamFromFile;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;

/** Given a logfile of 'messages' that were signed, verify them 'in real time', using the included timestamp. Used to benchmark verification. */

public class ReplaySavedMessagesRealtimeThread extends MessageGeneratorThreadBase {
	final private IncomingMessageStreamFromFile input;
	long bias = -1; // WHat is the timestamp ('virtual clock') of the first message in the log?
	final String provider;
	
	/** Add new messages to the queue at the requested. 
	 * 
	 * @param rate Messages per second.
	 * */
	ReplaySavedMessagesRealtimeThread(QueueBase verifyqueue, FileInputStream fileinput, int rate, String provider) {
		super(verifyqueue,rate);
		if (fileinput == null)
			throw new Error();
		this.input = new IncomingMessageStreamFromFile(fileinput);
		this.provider = provider;
	}

	/** Setup the replay log, preloading the bias and the keys. */
	void setup(MultiplexedPublicKeyPrims prims) {
		// First pass: Preload all of the verification keys and get the timestamp bias.
		IncomingMessage im;
		while ((im = input.nextOnePass()) != null) {
			if (bias == -1)
				bias = im.getVirtualClock();
			prims.load(im.getSignatureBlob(),provider); // Fetch the signature blob.
		}
		input.resetStream();
	}
	
	@Override
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.
		Map<Integer,List<IncomingMessage>> heldMessages = new HashMap<Integer,List<IncomingMessage>>();
		
		while (!finished.get()) {
			IncomingMessage msg = input.nextOnePass();
			// We're at the end.
			if (msg == null)
				break;
			
			// STEP 1: Delay until the inject time equals this time.
			
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
			}

			// STEP 2: Add all the hosts whom we need to start buffering.
			if (msg.start_buffering != null)
				for (Integer i: msg.start_buffering)
					heldMessages.put(i, new ArrayList<IncomingMessage>());

			// STEP 3: Play the held-back messages.
			if (msg.end_buffering != null)
				for (Integer i: msg.end_buffering) {
					for (IncomingMessage j : heldMessages.get(i))
						queue.add(j);
					heldMessages.remove(i);

			// STEP 4: Play this message or hold it?
			if (heldMessages.containsKey(msg.getRecipientUser()))
				heldMessages.get(msg.getRecipientUser()).add(msg);
			else
				queue.add(msg);

			checkQueueOverflow();
			}
		}
		// We're closing the thread. Time to verify everything held up, right now.
		for (List<IncomingMessage> l : heldMessages.values())
			for (IncomingMessage j : l)
				queue.add(j);
			
		queue.finish();
	}
}
