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

package edu.rice.batchsig;

import java.util.ArrayList;

public class AsyncQueue<T> {
	private ArrayList<T> queue;

	public AsyncQueue() {
		initQueue();
	}

	/* (non-Javadoc)
	 * @see edu.rice.batchsig.SignerQueue#add(edu.rice.batchsig.Message)
	 */
	public synchronized void add(T message) {
		queue.add(message);
		this.notify();
		//System.out.println("QUEUE");
	}

	/** Get the current queue size */
	public synchronized int peekSize() {
		return queue.size();
	}
	
	private void initQueue() {
		queue = new ArrayList<T>(32);
	}

	/**
	 * Get the queued messages which need to be signed atomically. This
	 * permanently removes them from the queue and the invoker is responsible
	 * for processing (or re-queuing) the messages.
	 */
	protected ArrayList<T> atomicGetQueue() {
		ArrayList<T> oldqueue;
		synchronized (this) {
			oldqueue = queue;
			initQueue();
		}
		return oldqueue;
	}

	/** Suspend the calling thread until the queue is non-empty */
	public synchronized void suspendTillNonEmpty() {
		try {
			this.wait();
		} catch (InterruptedException e) {
		}
	}
}

