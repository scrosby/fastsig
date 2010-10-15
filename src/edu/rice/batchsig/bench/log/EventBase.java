package edu.rice.batchsig.bench.log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class EventBase {
	protected long timestamp;
	protected int recipient_user;

	public EventBase(long timestamp, int recipient_user) {
		this.timestamp = timestamp;
		this.recipient_user = recipient_user;
	}

	public int getRecipientUser() {
		return recipient_user;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long d) {
		this.timestamp = d;
	}
	static void sort(EventBase log[]) {
	Arrays.sort(log,new Comparator<EventBase>(){
		@Override
		public int compare(EventBase arg0, EventBase arg1) {
			return (int) (arg0.getTimestamp()-arg1.getTimestamp());
		}});
	}
	
	static abstract class IterBase<T> implements Iterator<T> {
		FileInputStream fileinput;
		final protected BufferedReader input;
		private T cached;

		IterBase(FileInputStream fileinput) {
			this.fileinput = fileinput;
			this.input = new BufferedReader(new InputStreamReader(fileinput));
		}
		
		@Override
		public boolean hasNext() {
			try {
				return cached != null || input.ready();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		/** Returns null if empty */
		@Override
		public T next() {
			try {
				T out;
				if (cached != null) {
					out = cached;
				} else {
					out = readOne();
				}
				cached = null;
				return out;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void remove() {
			throw new Error("Fail");
		}
		
		protected abstract T readOne() throws IOException;
	}
}