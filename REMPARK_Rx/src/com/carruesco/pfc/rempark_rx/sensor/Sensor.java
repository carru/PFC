package com.carruesco.pfc.rempark_rx.sensor;

import java.nio.ByteBuffer;

import android.util.Log;

public class Sensor {
	private static final String TAG = "Sensor";
	public static final int FRAME_LENGTH = 32;
	
	// Commands
	public static final byte[] samplingRate200HzCommand = {0x53, 0x41, 0x32, 0x30, 0x30, (byte) 0x99, (byte) 0x98}; // SA200
	public static final byte[] samplingRate10HzCommand = {0x53, 0x41, 0x30, 0x31, 0x30, (byte) 0x99, (byte) 0x98}; // SA010
	public static final byte[] sendThroughBTCommand = {0x42, 0x54, (byte) 0x99, (byte) 0x98}; // BT
	public static final byte[] enableCommand = {0x4F, 0x4E, (byte) 0x99, (byte) 0x98}; // ON
	public static final byte[] ackCommand = {0x41, 0x43, 0x4B, (byte) 0x99, (byte) 0x98}; // ACK
	public static final byte[] frameStart = {0x11, 0x22, 0x33, 0x44};
	
	public static MultiSample parse(byte[] frame) {
		if (!Sensor.frameIsValid(frame)) {
			Log.e(TAG, "Invalid frame!");
			Log.e(TAG, Integer.toHexString(frame[0]) + 
					Integer.toHexString(frame[1]) + 
					Integer.toHexString(frame[2]) + 
					Integer.toHexString(frame[3]));
			return null;
		}
		
		// Accelerometer
		ByteBuffer bBuffer = ByteBuffer.wrap(frame, 4, 28);
		double Ax = bBuffer.getShort() * 2.9412;
		double Ay = bBuffer.getShort() * 2.9412;
		double Az = bBuffer.getShort() * 2.9412;
		SensorSample accelerometerSample = new SensorSample(Ax, Ay, Az);
		
		// Gyroscope
		double Gx = ((bBuffer.getShort() * 3.3/4096) - 1.35) * 2000;
		double Gy = ((bBuffer.getShort() * 3.3/4096) - 1.35) * 2000;
		double Gz = ((bBuffer.getShort() * 3.3/4096) - 1.35) * 2000;
		SensorSample gyroscopeSample = new SensorSample(Gx, Gy, Gz);
		
		// Magnetometer
		double Mx = bBuffer.getShort() * 3.3/4096;
		double My = bBuffer.getShort() * 3.3/4096;
		double Mz = bBuffer.getShort() * 3.3/4096;
		SensorSample magnetometerSample = new SensorSample(Mx, My, Mz);
		
		return new MultiSample(accelerometerSample, magnetometerSample, gyroscopeSample);
	}
	
	public static boolean isACK(byte[] message) {
		for (int i=0; i<message.length; i++) {
			if (i == ackCommand.length) { return true; } // If we have already compared all the bytes
			if (message[i] != ackCommand[i]) { return false; }
		}
		
		// Should never get here
		return true;
	}
	
	public static boolean frameIsValid(byte[] frame) {
		// Check size
		if (frame.length != FRAME_LENGTH) { return false; }
		
		for (int i=0; i<frameStart.length; i++) {
			if (frame[i] != frameStart[i]) { return false; }
		}

		/*if (frame[0] != 0x11) { return false; }
		if (frame[1] != 0x22) { return false; }
		if (frame[2] != 0x33) { return false; }
		if (frame[3] != 0x44) { return false; }*/
		
		return true;
	}
}
