package edu.rice.batchsig.bench;

import java.util.concurrent.atomic.AtomicBoolean;

public class Tracker {
	public static Tracker singleton = new Tracker();
	Histogram batchsizehist = new Histogram();
	Histogram latencyhist = new Histogram();
	Histogram bytesizehist = new Histogram();
	AtomicBoolean aborting = new AtomicBoolean();
	public int signcount,verifycount, verifycount_cached, validated;
	public int idleforces;
	public int immediate,lazy;
	
	public static long init = System.currentTimeMillis();
	
	
	private Tracker() {
		reset();
	}
	
	public void enable() {
		latencyhist.enable();
		bytesizehist.enable();
		batchsizehist.enable();
	}
	
	public void reset() {
		bytesizehist.reset();
		latencyhist.reset();
		batchsizehist.reset();
		aborting.set(false);
		validated = signcount = verifycount = verifycount_cached = idleforces = 0 ;
		immediate = lazy = 0;
	}
	
	public void markAbort() {
		aborting.set(true);
	}

	public boolean isAborting() {
		return aborting.get();
	}
	
	void trackMsgBytesize(int i) {
		bytesizehist.add(i);
	}
	
	public void trackBatchSize(int i) {
		batchsizehist.add(i);
	}

	void trackLatency(int i) {
		latencyhist.add(i);
	}
	
	private void histline(Histogram hist, int i, int j) {
		if (hist.sum(i,hist.bucketcount) == 0)
			return;
		if (i+1==j)
			System.out.format("     % 4d : %d\n",i,hist.sum(i,j));
		else
			System.out.format("% 4d-% 4d : %d\n",i,j,hist.sum(i,j));	
	}
	
	protected void printlineset(Histogram hist) {
		for (int i=0 ; i < 50 ; i++)
			histline(hist,i,i+1);
		histline(hist,50,60);
		histline(hist,60,70);
		histline(hist,70,80);
		histline(hist,80,90);
		histline(hist,90,100);
		histline(hist,100,200);
		histline(hist,200,300);
		histline(hist,300,400);
		histline(hist,400,500);
		histline(hist,500,1000);
		histline(hist,1000,2000);
		histline(hist,2000,3000);
		histline(hist,3000,4000);
		histline(hist,4000,4001);		
	}
	public String cryptoreport() {
	    long now = System.currentTimeMillis();
		return String.format("(%.3f) MSGS:%d, Sign=%d  VTotal=%d VCached=%d  Imm=%d Lazy=%d  Idle=%d",(now-init)/1000.0,validated,signcount,verifycount,verifycount_cached,immediate,lazy,idleforces);
	}
	
	public void print(String prefix) {
		System.out.format("MSGS: %d Immed:%d\n",validated,immediate);
		System.out.format("bsize: Rate=%s  N=%d  Avg=%f  Max=%d\n",prefix,batchsizehist.n,(double)batchsizehist.sum/batchsizehist.n,batchsizehist.max);
		printlineset(batchsizehist);

		System.out.format("crypt: Rate=%s  N=%d  Sign=%d  VTotal=%d VCached=%d\n",prefix,latencyhist.n,signcount,verifycount,verifycount_cached);
		System.out.format("laten: Rate=%s  N=%d  Avg=%f  Max=%d\n",prefix,latencyhist.n,(double)latencyhist.sum/latencyhist.n,latencyhist.max);
		printlineset(latencyhist);
		System.out.format("proof: Rate=%s  N=%d  Avg=%f  Max=%d\n",prefix,bytesizehist.n,(double)bytesizehist.sum/latencyhist.n,bytesizehist.max);
		
		
	}
}
