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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manage an asynchronous queue of messages to process.
 * 
 * Messages can be added to the queue at any time, and the contents can be
 * atomically fetched out. (which empties the queue)
 * 
 * @author scrosby
 * 
 * @param <T>
 */
public class AsyncQueue<T> {
	private ArrayList<T> queue;
	AtomicInteger size = new AtomicInteger(0);
	
	public AsyncQueue() {
		initQueue();
	}

	/** Add the given message to the queue */
	public synchronized void add(T message) {
		queue.add(message);
		size.addAndGet(1);
		this.notify();
	}

	/** Get the current queue size.  */
	public int peekSize() {
		return size.get();
	}

	/** Reset and start with a new empty queue */
	private void initQueue() {
		queue = new ArrayList<T>(32);
		size.set(0);
	}

	/**
	 * Get the set of queued messages atomically and empty the queue. 
	 * 
	 * This permanently removes them from the queue and the invoker is responsible
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
