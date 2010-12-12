package edu.rice.batchsig.lazy;

import java.util.LinkedHashMap;
import java.util.Map;


class ExpirationManager<T> extends LinkedHashMap<T,T> {
	private static final long serialVersionUID = 1L;
	private int size_limit;

	ExpirationManager(int size_limit) {
		this.size_limit = size_limit;
	}

	public void add(T item) {
		this.put(item,item);
	}

	protected boolean removeEldestEntry(Map.Entry<T,T> eldest) {
		if (this.size() > size_limit) {
			return true;
		}
		return false;
	}
}

