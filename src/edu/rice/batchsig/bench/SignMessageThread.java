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

import edu.rice.batchsig.QueueBase;

/** Given a logfile of 'messages' to be signed, play them. Each message has an arrival timestamp. */

public class SignMessageThread extends Thread {
	private int epochlength;
	private QueueBase signqueue;
	private AtomicBoolean finished = new AtomicBoolean(false);
	
	
	SignMessageThread(QueueBase signqueue, int epochtime) {
		this.signqueue = signqueue;
		this.epochlength = epochtime;
	}
	

	public void shutdown() {
		this.interrupt();
		finished.set(true);
	}
	
	
	@Override
	public void run() {
		while (!finished.get()) {
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
