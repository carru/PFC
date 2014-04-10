package com.carruesco.pfc.rempark_rx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.carruesco.pfc.rempark_rx.sensor.MultiSample;
import android.content.Context;
import android.os.Environment;
import android.text.format.Time;

// Writes samples to a log file in external storage to process them on a PC
// Keeps received samples in ArrayLists and writes them when the whole
// supersample (a sample from each sensor) is received
public class SamplesLogger {
	private Context context;
	private BufferedWriter bufferedWriter;
	
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
	
	public synchronized void write(MultiSample sample) {
		try {
			bufferedWriter.write(Double.toString(sample.accelerometer.getX()) + ";" 
					+ Double.toString(sample.accelerometer.getY()) + ";"
					+ Double.toString(sample.accelerometer.getZ()) + ";" +

					Double.toString(sample.magnetometer.getX()) + ";"
					+ Double.toString(sample.magnetometer.getY()) + ";"
					+ Double.toString(sample.magnetometer.getZ()) + ";" +

					Double.toString(sample.gyroscope.getX()) + ";"
					+ Double.toString(sample.gyroscope.getY()) + ";"
					+ Double.toString(sample.gyroscope.getZ()) + ";\n");
			samplesCounter++;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
