package edu.rice.batchsig.bench;

public class Tracker {
	public static Tracker singleton = new Tracker();
	
	Histogram latencyhist = new Histogram();
	Histogram sizehist = new Histogram();
	boolean aborting;
	int signcount,verifycount;
	
	private Tracker() {
		reset();
	}
	
	public void enable() {
		latencyhist.enable();
		sizehist.enable();
	}
	
	public void reset() {
		sizehist.reset();
		latencyhist.reset();
		aborting = false;
		signcount = verifycount = 0;
	}
	
	public void markAbort() {
		aborting = true;
	}

	public boolean isAborting() {
		return aborting;
	}
	
	void trackSize(int i) {
		sizehist.add(i);
	}

	void trackLatency(int i) {
		latencyhist.add(i);
	}
	
	private void latencyhistline(Histogram hist, int i, int j) {
		if (latencyhist.sum(i,hist.bucketcount) == 0)
			return;
		if (i+1==j)
			System.out.format("     % 4d : %d\n",i,hist.sum(i,j));
		else
			System.out.format("% 4d-% 4d : %d\n",i,j,hist.sum(i,j));	
	}

	
	protected void print(String prefix) {
		System.out.format("lat: Rate=%s  N=%d  Avg=%f  Max=%d\n",prefix,latencyhist.n,(double)latencyhist.sum/latencyhist.n,latencyhist.max);
		System.out.format("siz: Rate=%s  N=%d  Avg=%f  Max=%d\n",prefix,sizehist.n,(double)sizehist.sum/latencyhist.n,sizehist.max);
		for (int i=0 ; i < 50 ; i++)
			latencyhistline(latencyhist,i,i+1);
		latencyhistline(latencyhist,50,60);
		latencyhistline(latencyhist,60,70);
		latencyhistline(latencyhist,70,80);
		latencyhistline(latencyhist,80,90);
		latencyhistline(latencyhist,90,100);
		latencyhistline(latencyhist,100,200);
		latencyhistline(latencyhist,200,300);
		latencyhistline(latencyhist,300,400);
		latencyhistline(latencyhist,400,500);
		latencyhistline(latencyhist,500,1000);
		latencyhistline(latencyhist,1000,2000);
		latencyhistline(latencyhist,2000,3000);
		latencyhistline(latencyhist,3000,4000);
		latencyhistline(latencyhist,4000,4001);
	}
}
