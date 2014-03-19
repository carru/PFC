package com.example.sensortagrx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

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
	
	// Number of supersamples written
	private int superSamplesCounter;
	
	public SamplesLogger(Context context) {
		superSamplesCounter = 0;
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
		Log.i("Logger", "SuperSamples written: " + Integer.toString(superSamplesCounter));
	}
	
	// Delete all the logs in the folder
	public static void deleteAllLogs(Context c) {
		File root = new File(c.getExternalFilesDir(null), "");
		
		// All files in our directory
		File[] files = root.listFiles();
		
		// Loop through all files and delete them
		for (File file : files) {
			file.delete();
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
		// Do we have all the samples of a supersample?
		// Assumes same sampling rate for all sensors
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
				
				superSamplesCounter++;
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
	private String getFileName() {
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
	
	// If file exists (unlikely because filename is a timestamp) it overwrites
	private void createLogFile() {
		String fileName = getFileName();
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
}
