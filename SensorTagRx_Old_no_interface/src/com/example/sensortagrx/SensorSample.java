package com.example.sensortagrx;

// This object contains the 3 floats of a single sample from a given sensor
public class SensorSample {
	private float x, y, z;
	
	public SensorSample() {
		x = 0;
		y = 0;
		z = 0;
	}
	
	public SensorSample(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}
	
	public float getZ() {
		return z;
	}
}
