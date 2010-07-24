package edu.rice.batchsig.bench;

public abstract class Tracker {
	boolean disabled;
	protected int bucketcount;

	public void reset() {
		buckets = new long[bucketcount];
		sum = 0;
		n = 0;
		max = -1;
		disabled = true;
	}

	protected long buckets[];
	protected long sum;
	protected long n;
	protected long max;

	public Tracker() {
		this(4001);
	}
	
	private Tracker(int bucketcount) {
		this.bucketcount = bucketcount;
		reset();
	}

	void enable() {
		disabled = false;
	}

	public void add(int time) {
		if (disabled)
			return;
		sum += time;
		n++;
		if (time > max) 
			max = time;
		if (time >= bucketcount) {
			time = bucketcount-1;
		}
		buckets[time]++;
	}

	long sum(int i, int j) {
		long out=0;
		for (int k=i ; k < j ; k++)
			out += buckets[k];
		return out;
	}

	void histLine(int i, int j) {
		if (sum(i,bucketcount) == 0)
			return;
		if (i+1==j)
			System.out.format("     % 4d : %d\n",i,sum(i,j));
		else
			System.out.format("% 4d-% 4d : %d\n",i,j,sum(i,j));	
	}

	protected void print() {
		System.out.format("N=%d  Avg=%f  Max=%d\n",n,(double)sum/n,max);
		for (int i=0 ; i < 50 ; i++)
			histLine(i,i+1);
		histLine(50,60);
		histLine(60,70);
		histLine(70,80);
		histLine(80,90);
		histLine(90,100);
		histLine(100,200);
		histLine(200,300);
		histLine(300,400);
		histLine(400,500);
		histLine(500,1000);
		histLine(1000,2000);
		histLine(2000,3000);
		histLine(3000,4000);
		histLine(4000,4001);
	}

}