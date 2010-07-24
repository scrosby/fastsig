package edu.rice.batchsig.bench;

public class LatencyTracker extends Tracker {
	boolean aborting;
	boolean disabled;
	
	
	@Override
	public void reset() {
		super.reset();
		aborting = false;
	}
	
	public void markAbort() {
		aborting = true;
	}

	public boolean isAborting() {
		return aborting;
	}
	
	void msgSize(int i) {
	}
}
