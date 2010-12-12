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

import java.util.concurrent.atomic.AtomicBoolean;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.OMessage;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.ShutdownableThread;
import edu.rice.batchsig.SuspendableProcessQueue;

/** Run processs over a at a given minimum epoch time. */

public class ProcessQueueThread extends ShutdownableThread {
	private int epochlength;
	private SuspendableProcessQueue<? extends Message> signqueue;
		
	ProcessQueueThread(SuspendableProcessQueue<? extends Message> signqueue, int epochtime) {
		this.setName("ProcessQueue");
		if (signqueue == null)
			throw new Error("NO signqueue given");
		this.signqueue = signqueue;
		this.epochlength = epochtime;
	}
	
	
	@Override
	public void run() {
		while (!isShuttingdown()) {
			//System.out.println("SigningLoop");
			long epochstart = System.currentTimeMillis();
			signqueue.suspendTillNonEmpty();
			signqueue.process();

			long now = System.currentTimeMillis();
			while (now < epochlength+epochstart) {
				try {
					//System.out.println("SLEEPyy:"+(epochlength+epochstart-now));
					Thread.sleep(epochlength+epochstart-now);
				} catch (InterruptedException e) {
				}
				now = System.currentTimeMillis();
			}
		}
		//System.out.println("FinishSigning");
		// Handle any trailing unprocessed stuff
		signqueue.process();
		signqueue.finish();
	}
	
}
