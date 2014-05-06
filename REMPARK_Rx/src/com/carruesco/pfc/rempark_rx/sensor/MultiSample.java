package com.carruesco.pfc.rempark_rx.sensor;


// Contains samples of all three sensors
public class MultiSample {
	public SensorSample accelerometer;
	public SensorSample magnetometer;
	public SensorSample gyroscope;
	
	public MultiSample(SensorSample accelerometer, SensorSample magnetometer, SensorSample gyroscope) {
		this.accelerometer = accelerometer;
		this.magnetometer = magnetometer;
		this.gyroscope = gyroscope;
	}
	
	public MultiSample(double[] data) {
		accelerometer = new SensorSample(data[0], data[1], data[2]);
		magnetometer = new SensorSample(data[3], data[4], data[5]);
		gyroscope = new SensorSample(data[6], data[7], data[8]);
	}
	
	public MultiSample() {
		// Empty sample
		this(new double[]{0,0,0, 0,0,0, 0,0,0});
	}
	
	public static double[] getDataVector(MultiSample sample) {
		return new double[]{ sample.accelerometer.getX(), 
				sample.accelerometer.getY(), 
				sample.accelerometer.getZ(), 
				sample.magnetometer.getX(), 
				sample.magnetometer.getY(), 
				sample.magnetometer.getZ(), 
				sample.gyroscope.getX(), 
				sample.gyroscope.getY(), 
				sample.gyroscope.getZ() };
	}
}
