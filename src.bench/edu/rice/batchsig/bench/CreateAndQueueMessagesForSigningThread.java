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
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.OMessage;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.historytree.generated.Serialization.MessageData;

/** Given a logfile of 'messages' to be signed, play them. Each message has an arrival timestamp. */

public class CreateAndQueueMessagesForSigningThread extends MessageGeneratorThreadBase<OMessage> {
	final private CodedOutputStream output;
	long seqno = 0;
	long rate = 0;
	/** Add new messages to the queue at the requested. 
	 * 
	 * @param rate Messages per second.
	 * */
	CreateAndQueueMessagesForSigningThread(ProcessQueue<OMessage> signqueue, CodedOutputStream output, int rate) {
		super(signqueue,rate);
		this.output = output;
		this.rate = rate;
	}

	static final Integer DEFAULT_USER = new Integer(31415926);
	
	@Override
	public void run() {
		long initTime = System.currentTimeMillis(); // When we started.
		long insertedNum = 0;
		while (!isShuttingdown()) {
			long now = System.currentTimeMillis();
			long deltaTime = now-initTime; // Ok. In DELTA ms, we should have inserted..
			long targetNum = deltaTime*rate/1000; // this many messages.
			if (insertedNum < targetNum) {
				while (insertedNum < targetNum) {
					insertedNum++;
					queue.add(new OutgoingMessage(output,String.format("Msg:%d",seqno++).getBytes(),new Object(),DEFAULT_USER));
					checkQueueOverflow();
				}
			} else { 
				// (insertNum+1)/rate*1000  (but we rearrange for better roundoff
				long wakeupTime = initTime + (insertedNum+1)*1000/rate;
				// Running ahead. Lets sleep for a little bit. 
				long sleepTime = wakeupTime - now;
				if (sleepTime > 0)
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
					}
			}
		}
		queue.finish();
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
