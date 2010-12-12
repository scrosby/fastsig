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
import java.util.HashSet;
import java.util.Set;

import com.google.protobuf.ByteString;

import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.batchsig.bench.IncomingMessageStreamFromFile;
import edu.rice.batchsig.bench.MessageGeneratorThreadBase;
import edu.rice.batchsig.bench.Tracker;

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
			if (im.getSignatureBlob() != null)
				prims.load(im.getSignatureBlob()); // Fetch the signature blob.
		}
		input.resetStream();
	}
	
	@Override
	public void run() {
		int count = 0;
		Set<Integer> loggedOnUsers = new HashSet<Integer>();
		long initTime = System.currentTimeMillis(); // When we started.
		long msgOffsetTime = 0;
		while (!isShuttingdown()) {
			IncomingMessage msg = input.nextOnePass();
			// We're at the end.
			if (msg == null) {
				System.out.println("EOF");
				break;
			}
			
			// STEP 1: Delay until the inject time equals this time.
			
			// Problem is that loginlogoff messages do NOT have timestamps. Borrow the timestamp from the prior message.
		    if (msg.getVirtualClock() >0)
		    	msgOffsetTime = msg.getVirtualClock();
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
			count++;
			if (now - injectTime > 100)
				System.err.format("Runnign behind %dms on message #%d injection  %d  %d   %d\n",now-injectTime,count,now-initTime,injectTime-initTime,msgOffsetTime);
			
			//msg.resetCreationTimeNull(); // So that we correct for the wait time above.
			
			if (msg.getData() != null) {
				//System.out.println("Bad message "+msg.getVirtualClock() + "data" + msg.getData().length);
				//if (msg.getSignatureBlob() == null) {System.out.println("Interesting...."); Thread.dumpStack();}
				//if (msg.getAuthor() == null) {System.out.println("Interesting...."); Thread.dumpStack();}
				// DEBUG: Only report messages from ONE author.
				//if (!((ByteString)msg.getAuthor()).toStringUtf8().equals("Signer4"))
				//	continue;
				
				if (loggedOnUsers.contains(msg.getRecipientUser())) {
					//lazyqueue.addForced(msg);
					Tracker.singleton.immediate++;
					lazyqueue.add(msg);
					lazyqueue.forceUser((Integer)msg.getRecipientUser());
				} else {
					Tracker.singleton.lazy++;
					lazyqueue.add(msg);
				}
			}
			int tot_users = 0;
			tot_users += msg.start_buffering == null ? 0 : msg.start_buffering.size();
			tot_users += msg.end_buffering == null ? 0 : msg.end_buffering.size();

			if (tot_users > 100)
				System.err.format("Lots of messages changing buffering %d \n",tot_users);
			// STEP 3: Record any users that have just logged off.
			if (msg.start_buffering != null) {
				for (Integer i: msg.start_buffering) {
					loggedOnUsers.remove(i);
				}
			}
			// STEP 3: Force any users that have just logged on.
			if (msg.end_buffering != null) {
				for (Integer i: msg.end_buffering) {
					loggedOnUsers.add(i);
					lazyqueue.forceUser(i);
				}
			}
			checkQueueOverflow();
			//System.out.println("Iterating replay loop");
		}
		queue.finish();
	}
}
