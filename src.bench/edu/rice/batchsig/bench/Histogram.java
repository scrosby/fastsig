package edu.rice.batchsig.bench;

public class Histogram {
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

	public Histogram() {
		this(4001);
	}
	
	private Histogram(int bucketcount) {
		this.bucketcount = bucketcount;
		reset();
	}

	void enable() {
		disabled = false;
	}

	public void add(int time) {
		if (disabled)
			return;
		if (time < 0)
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

	public long sum(int i, int j) {
		long out=0;
		for (int k=i ; k < j ; k++)
			out += buckets[k];
		return out;
	}

}
