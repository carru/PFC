package com.carruesco.pfc.rempark_rx;

// Contains samples of all three sensors
public class MultiSample {
	private SensorSample accelerometer;
	private SensorSample magnetometer;
	private SensorSample gyroscope;
	
	public MultiSample(SensorSample accelerometer, SensorSample magnetometer, SensorSample gyroscope) {
		this.accelerometer = accelerometer;
		this.magnetometer = magnetometer;
		this.gyroscope = gyroscope;
	}
}
