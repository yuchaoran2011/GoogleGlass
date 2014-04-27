package edu.berkeley.camerademo;

public class Step {
	public double hdg;
	public double distance;
	public long tstamp;
	public Step(double hdg, double distance, long tstamp) {
		this.hdg = hdg;
		this.distance = distance;
		this.tstamp = tstamp;
	}
}
