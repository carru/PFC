package com.carruesco.pfc.rempark_rx.sensor;

public class SensorSample {
	private double x, y, z;
	
	public SensorSample() {
		x = 0;
		y = 0;
		z = 0;
	}
	
	public SensorSample(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public SensorSample(double[] data) {
		this.x = data[0];
		this.y = data[1];
		this.z = data[2];
	}
	
	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }
	
	public static double[] getDataVector(SensorSample sample) {
		return new double[]{sample.x, sample.y, sample.z};
	}
}
