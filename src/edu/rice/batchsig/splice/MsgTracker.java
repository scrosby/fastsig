package edu.rice.batchsig.splice;

import edu.rice.batchsig.Message;

public interface MsgTracker {

	public abstract void add(Message m);

	public abstract void remove(Message m);

	public abstract void force(Object user);

	public abstract boolean isLazy(Message m);

}