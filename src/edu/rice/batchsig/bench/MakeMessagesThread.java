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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString;

import edu.rice.batchsig.QueueBase;
import edu.rice.historytree.generated.Serialization.MessageData;

/** Given a logfile of 'messages' to be signed, play them. Each message has an arrival timestamp. */

public class MakeMessagesThread extends Thread {
	private int rate;
	private QueueBase signqueue;
	private LatencyTracker tracker;
	private AtomicBoolean finished = new AtomicBoolean(false);
	
	/** Add new messages to the queue at the requested. 
	 * 
	 * @param rate Messages per second.
	 * */
	MakeMessagesThread(QueueBase signqueue, LatencyTracker tracker, int rate) {
		this.signqueue = signqueue;
		this.rate = rate;
		this.tracker = tracker;
	}
	
	public void shutdown() {
		finished.set(true);
	}
	
	
	@Override
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.
		long insertedNum = 0;
		while (!finished.get()) {
			long now = System.currentTimeMillis();
			long deltaTime = now-initTime; // Ok. In DELTA ms, we should have inserted..
			long targetNum = deltaTime*rate/1000; // this many messages.
			if (insertedNum < targetNum) {
				while (insertedNum < targetNum) {
					insertedNum++;
					addToQueue();
				}
			} else { 
				// (insertNum+1)/rate*1000  (but we rearrange for better roundoff
				long wakeupTime = initTime + (insertedNum+1)*1000/rate;
				// Running ahead. Lets sleep for a little bit. 
				try {
					Thread.sleep(wakeupTime-now);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	static long lastErr = 0;
	static long skip = 0;
	
	long seqno = 0;
	void addToQueue() {
		seqno++;
		if (signqueue.peekSize() > rate) {
			skip++;
			if (System.currentTimeMillis() > lastErr + 1000) {
				System.err.format("Queue overfull(%d)\n",skip);
				if (skip > 1)
					tracker.markAbort();
				skip=0; lastErr = System.currentTimeMillis();
			}
		}
		signqueue.add(new OutgoingMessage(tracker,null,String.format("Msg:%d",seqno++).getBytes(),new Object()));
	}

	void replayQueue() {
	}
	
	
	/* Future code for an unimplemented message generator subclass.
	static Random rand = new Random();
	static Object objs[] = new Object[20];
	static {
		for (int i = 0 ; i < objs.length ; i++)
			objs[i] = new Object();
	}

	Object pickSource() {
		return new Object();
		// TODO: return objs[rand.nextInt(objs.length)];
	}
    */
}
