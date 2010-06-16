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

public class SignQueueProcessor implements Runnable {
	private int epochlength;
	private QueueBase signqueue;
	private AtomicBoolean finished = new AtomicBoolean(false);
	
	
	SignQueueProcessor(QueueBase signqueue, int epochtime) {
		this.signqueue = signqueue;
		this.epochlength = epochtime;
	}
	

	public void shutdown() {
		finished.set(true);
	}
	
	
	@Override
	public void run() {
		while (!finished.get()) {
			long epochstart = System.currentTimeMillis();
			signqueue.process();

			long now = System.currentTimeMillis();
			while (now < epochlength+epochstart) {
				try {
					Thread.sleep(epochlength+epochstart-now);
				} catch (InterruptedException e) {
				}
				now = System.currentTimeMillis();
			}
		}
		signqueue.process();
	}
}
