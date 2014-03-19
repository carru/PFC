package com.carruesco.pfc.sensortagrx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.carruesco.pfc.sensortagrx.sensors.SensorSample;

import android.content.Context;
import android.os.Environment;
import android.text.format.Time;

// Writes samples to a log file in external storage to process them on a PC
// Keeps received samples in ArrayLists and writes them when the whole
// supersample (a sample from each sensor) is received
public class SamplesLogger {
	private Context context;
	private BufferedWriter bufferedWriter;
	
	// ArrayLists for sensor samples
	private ArrayList<SensorSample> accelerometerSamples;
	private ArrayList<SensorSample> magnetometerSamples;
	private ArrayList<SensorSample> gyroscopeSamples;
	
	// To check if it is ready and working (can write)
	private boolean isReady;
	
	// Number of samples written
	private int samplesCounter;
	
	// File name to use instead of default
	private String fileName;
	
	public SamplesLogger(Context context) {
		this(context, null);
	}
	
	public SamplesLogger(Context context, String fileName) {
		this.fileName = fileName;
		samplesCounter = 0;
		this.context = context;
		isReady = false;
		
		// Check if filesystem is writable
		if (!isExternalStorageWritable()) { return; }
		
		// Initialize lists
		accelerometerSamples = new ArrayList<SensorSample>();
		magnetometerSamples = new ArrayList<SensorSample>();
		gyroscopeSamples = new ArrayList<SensorSample>();
		
		// Create log file
		createLogFile();
	}
	
	public boolean isReady() { return isReady; }
	
	public void close() {
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeAccelerometerSample(SensorSample sample) {
		accelerometerSamples.add(sample);
		write();
	}
	
	public void writeMagnetometerSample(SensorSample sample) {
		magnetometerSamples.add(sample);
		write();
	}
	
	public void writeGyroscopeSample(SensorSample sample) {
		gyroscopeSamples.add(sample);
		write();
	}
	
	private synchronized void write() {
		// Do we have all the samples of each sensor?
		// Assumes same sampling rate for all sensors!
		if (!accelerometerSamples.isEmpty() && !magnetometerSamples.isEmpty() && !gyroscopeSamples.isEmpty()) {
			SensorSample accelerometerSample = accelerometerSamples.remove(0);
			SensorSample magnetometerSample = magnetometerSamples.remove(0);
			SensorSample gyroscopeSample = gyroscopeSamples.remove(0);
			
			try {
				bufferedWriter.write(Float.toString(accelerometerSample.getX()) + ";" +
									 Float.toString(accelerometerSample.getY()) + ";" +
									 Float.toString(accelerometerSample.getZ()) + ";" +
									
									 Float.toString(magnetometerSample.getX()) + ";" +
									 Float.toString(magnetometerSample.getY()) + ";" +
									 Float.toString(magnetometerSample.getZ()) + ";" +
									
									 Float.toString(gyroscopeSample.getX()) + ";" +
									 Float.toString(gyroscopeSample.getY()) + ";" +
									 Float.toString(gyroscopeSample.getZ()) + ";\n");
				samplesCounter++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// Checks if external storage is available for read and write
	private boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	// Return timestamp filename in readable format
	private String getDefaultFileName() {
		Time now = new Time();
		now.setToNow();
		return now.format2445();
	}
	
	private void writeHeader() {
		String header;
		header = "# SensorTagRx log file\n" +
				 "# Format: Ax;Ay;Az;Mx;My;Mz;Gx;Gy;Gz\n" +
				 "# Where A is the accelerometer, M the magnetometer and G the gyroscope\n" +
				 "# x, y and z are the three axis of each sensor\n" +
				 "# Units: g, uT and deg/s\n" + 
				 "# Start of log:\n";
		
		try {
			bufferedWriter.write(header);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// If file exists, it overwrites
	private void createLogFile() {
		// Use default file name if not specified
		if (fileName == null) { fileName = getDefaultFileName(); }
		
		File file = new File(context.getExternalFilesDir(null), fileName + ".log");
		
		// BufferedWriter for performance
	    try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			writeHeader();
			isReady = true;
		} catch (IOException e) {
			isReady = false;
			e.printStackTrace();
		}
	}
	
	public String getFileName() { return fileName; }
	public int getNumberOfWrittenSamples() { return samplesCounter; }
}
