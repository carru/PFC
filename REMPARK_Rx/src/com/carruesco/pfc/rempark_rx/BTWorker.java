package com.carruesco.pfc.rempark_rx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.carruesco.pfc.rempark_rx.sensor.MultiSample;
import com.carruesco.pfc.rempark_rx.sensor.Sensor;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

public class BTWorker extends Thread {
	private String TAG = "BTWorker";
	
	// Log writer
    private SamplesLogger logger;
    private Time loggerStartTime = new Time();
    
	// Buffer for the read operations (bytes)
	private final int READ_BUFFER_SIZE = 10240;
	private boolean multipleSamples = false;
	
	// To update connection state when we start receiving data
	private boolean notifyIsConnected;
	
	// Context to send broadcasts
	private Context context;
	// Broadcast samples?
	public static boolean broadcastSamples = false;
	// Samples counter
	private int count;
	// Rate at which to send samples to the live data fragment in Hz (not accurate)
	private final int LIVE_DATA_REFRESH_RATE = 20;
	private int samplingRate = 200;
	
	// Magnetometer calibration
	//public static boolean magnetometerIsCalibrating = false;
	//private SensorSample magnetometerOffset = new SensorSample(0, 0, 0);
	
	// Connection states
	private final int CONNECTED = 1;
	private final int FAILED = 2;
	
	// Socket and streams
	private BluetoothSocket mmSocket;
	private InputStream mmInStream;
	private OutputStream mmOutStream;
	
	public BTWorker(Context context, BluetoothSocket mmSocket) {
		this.context = context;
		this.mmSocket = mmSocket;
	}
	
	@Override
	public void run() {
		try { mmSocket.connect(); }
		catch (IOException e1) {
			Log.e(TAG, "Error connecting socket");
			updateConnectionState(FAILED);
			return;
		}
        Log.i(TAG, "Socket opened");
		
        // Get streams
        Log.i(TAG, "Opening streams");
        try {
        	mmInStream = mmSocket.getInputStream();
        	mmOutStream = mmSocket.getOutputStream();
        } catch (IOException e) {
        	Log.e(TAG, "Error opening streams");
			updateConnectionState(FAILED);
			return;
        }
        Log.i(TAG, "Streams opened");
        
        if (!setupSensor()) {
        	Log.e(TAG, "Error setting up sensor");
			updateConnectionState(FAILED);
			return;
        }
        
        // Everything OK, sensor is now connected and  should start sending data
        Log.i(TAG, "Setup complete. Sensor should now start sending data");
        notifyIsConnected = true;
        
        // Start infinite reading loop
		readLoop();
	}
	
	private void readLoop() {
		final int FRAME_SIZE = 32;
		int i;
		
		// Buffer to read data
		byte[] readBuffer = new byte[READ_BUFFER_SIZE];
		int bytesRead;
		multipleSamples = false;
		
		// ArrayList to store read bytes
		ArrayList<Byte> rawBytes = new ArrayList<Byte>();
		
		// Frame of FRAME_SIZE bytes
		byte[] frame = new byte[FRAME_SIZE];
		
		// Received sample
		MultiSample sample = new MultiSample();
		count = 0;
		
		try {
			while(true) {
				if (interrupted()) { Log.d(TAG, "BTWorker has been interrupted"); return; }
				bytesRead = mmInStream.read(readBuffer);
				if (bytesRead > FRAME_SIZE) { multipleSamples = true; }
				
				// DEBUG
				//Log.d(TAG, "Read " + bytesRead + " bytes");
				
				// Update connection state
				if (notifyIsConnected) {
					notifyIsConnected = false;
					updateConnectionState(CONNECTED);
				}
				
				// Add read bytes to list
				for (i=0; i<bytesRead; i++) { rawBytes.add(readBuffer[i]); }
				
				// Do we have a full frame?
				while (rawBytes.size()>=FRAME_SIZE) {
					// Get frame from ArrayList
					for (i=0; i<FRAME_SIZE; i++) { frame[i] = rawBytes.remove(0); }
					
					// Parse frame
					//sample = Sensor.parse(frame);
					Sensor.parse(frame, sample);
					if (sample == null) {
						Log.e(TAG, "Error getting data from frame");
						continue;
					}
					
//					if (magnetometerIsCalibrating) {
//						magnetometerIsCalibrating = false;
//						magnetometerOffset = new SensorSample(sample.magnetometer);
//					}
//					sample.magnetometer.applyOffset(magnetometerOffset);
					
					// Log samples
					if (isLogging()) {
						logger.write(sample);
					}
					
					// Broadcast sample. Don't broadcast when there were too many samples at once
					if (multipleSamples) { multipleSamples = false; }
					else if (broadcastSamples) {
						count++;
						if (count >= samplingRate/LIVE_DATA_REFRESH_RATE) {
							count = 0;
							broadCastSample(sample);
						}
					}
				}
			}
		} catch (IOException e) { Log.d(TAG, "Exception in readLoop()"); return; }
	}
	
	private void broadCastSample(MultiSample sample) {
		Intent intent = new Intent(BTService.ACTION_DATA_AVAILABLE);
		intent.putExtra(BTService.VALUE, MultiSample.getDataVector(sample));
    	LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	private void write(byte[] bytes) {
        try { mmOutStream.write(bytes); }
        catch (IOException e) { }
    }
	
	private int read(byte[] buffer) {
    	int bytes;
    	
    	try { bytes = mmInStream.read(buffer); }
    	catch (IOException e) { return 0; }
    	
		return bytes;
    }
	
	private boolean setupSensor() {
		samplingRate = Common.sharedPref.getInt("key_sampling_rate", 200);
		
		// To read ACKs
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        
        // Set sampling rate
        Log.i(TAG, "Setting sample rate: " + samplingRate + " Hz");
        write(Sensor.getSACommand(samplingRate));
        // Read ACK
		read(buffer);
        if (!Sensor.isACK(buffer)) {
        	Log.e(TAG, "Error setting sample rate");
        	return false;
        }
		
        // Set to send through BT
        Log.i(TAG, "Setting to send through BT");
        write(Sensor.sendThroughBTCommand);
        // Read ACK
        read(buffer);
        if (!Sensor.isACK(buffer)) {
        	Log.e(TAG, "Error setting to send through BT");
        	return false;
        }
        
        // Enable
        Log.i(TAG, "Enabling sensor");
        write(Sensor.enableCommand);
        // Read ACK
        read(buffer);
        if (!Sensor.isACK(buffer)) {
        	Log.e(TAG, "Error enabling sensor");
        	return false;
        }
		
		return true;
	}
	
	private void updateConnectionState(int state) {
		Intent intent;
		
		switch (state) {
		case CONNECTED:
			Common.isConnected = true;
        	Common.isConnecting = false;
			intent = new Intent(BTService.ACTION_CONNECTED);
        	LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
			break;
		case FAILED:
			Common.isConnected = false;
        	Common.isConnecting = false;
			intent = new Intent(BTService.ACTION_DISCONNECTED);
        	LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
			break;
		}
	}
	
	public boolean startLogger() {
    	// Prepare log writer
    	logger = new SamplesLogger(context);
    	if (!logger.isReady()) { return false; }
    	loggerStartTime.setToNow();
    	return true;
    }
    
    public boolean startLogger(String fileName) {
    	// Prepare log writer
    	logger = new SamplesLogger(context, fileName);
    	if (!logger.isReady()) { return false; }
    	loggerStartTime.setToNow();
    	return true;
    }
    
    public void stopLogger() {
    	if (logger != null) {
    		logger.close();
    		logger = null;
    	}
    }
    
    public boolean isLogging() {
    	if (logger == null) { return false; }
    	else { return logger.isReady(); }
    }
    
    public String getLoggerFileName() {
    	if (logger == null) { return null; }
    	else { return logger.getFileName(); }
    }
    
    public int getLoggerNumberOfSamples() {
    	if (logger == null) { return -1; }
    	else { return logger.getNumberOfWrittenSamples(); }
    }
    
    public Time getLoggerStartTime() { return loggerStartTime; }
}
