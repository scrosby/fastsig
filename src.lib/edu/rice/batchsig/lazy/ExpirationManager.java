package edu.rice.batchsig.lazy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Handle a queue where if the the number of entries exceeds a limit, I identify and return the oldest entry. */
@SuppressWarnings("serial")
abstract class ExpirationManager<T> extends LinkedHashMap<T, T> {
	/** The maximum number of elements before we start expiring. */
	private int size_limit;

	ExpirationManager(int size_limit) {
		this.size_limit = size_limit;
	}

	/** Add an element for expiration. */
	public void add(T item) {
		this.put(item, item);
	}

	/** Is the oldest entry ready for expiration? If so, return true, so subclasses will handle the callback. */
	protected boolean removeEldestEntry(Map.Entry<T, T> eldest) {
		if (this.size() > size_limit) {
			expire(eldest.getKey());
			return true;
		}
		return false;
	}
	
	/** Invoked on the oldest element, if there are too many elements in the expiration queue. */
	protected abstract void expire(T element);
}

