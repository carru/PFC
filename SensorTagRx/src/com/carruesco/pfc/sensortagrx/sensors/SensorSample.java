package com.carruesco.pfc.sensortagrx.sensors;

public class SensorSample {
	private float x, y, z;
	private String sensorName;
	
	public SensorSample() {
		x = 0;
		y = 0;
		z = 0;
		sensorName = null;
	}
	
	public SensorSample(float x, float y, float z, String sensorName) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.sensorName = sensorName;
	}
	
	public SensorSample(float[] data, String sensorName) {
		this.x = data[0];
		this.y = data[1];
		this.z = data[2];
		this.sensorName = sensorName;
	}
	
	public float getX() { return x; }
	public float getY() { return y; }
	public float getZ() { return z; }
	public String getName() { return sensorName; }
	
	public static float[] getDataVector(SensorSample sample) {
		return new float[]{sample.x, sample.y, sample.z};
	}
}
