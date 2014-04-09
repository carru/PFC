package com.carruesco.pfc.rempark_rx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.carruesco.pfc.rempark_rx.sensor.MultiSample;
import com.carruesco.pfc.rempark_rx.sensor.Sensor;
import com.carruesco.pfc.rempark_rx.sensor.SensorSample;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BTWorker extends Thread {
	private String TAG = "BTWorker";
	
	private final int READ_BUFFER_SIZE = 512;
	
	// To update connection state when we start receiving data
	private boolean notifyIsConnected;
	
	// Context to send broadcasts
	Context context;
	// Broadcast samples?
	public static boolean broadcastSamples = false;
	
	// Magnetometer calibration
	public static boolean magnetometerIsCalibrating = false;
	private SensorSample magnetometerOffset = new SensorSample(0, 0, 0);
	
	// Connection states
	private final int CONNECTED = 1;
	private final int FAILED = 2;
	
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
		
		// ArrayList to store read bytes
		ArrayList<Byte> rawBytes = new ArrayList<Byte>();
		
		// Frame of FRAME_SIZE bytes
		byte[] frame = new byte[FRAME_SIZE];
		
		try {
			while(true) {
				if (interrupted()) { return; }
				bytesRead = mmInStream.read(readBuffer);
				//Log.d(TAG, "Read: " + bytesRead + " bytes");
				
				// Update connection state
				if (notifyIsConnected) {
					notifyIsConnected = false;
					updateConnectionState(CONNECTED);
				}
				
				// Add read bytes to list
				for (i=0; i<bytesRead; i++) { rawBytes.add(readBuffer[i]); }
				//Log.d(TAG, "rawBytes: " + rawBytes.size());
				
				// Do we have a full frame?
				if (rawBytes.size()>=FRAME_SIZE) {
					// Get frame from ArrayList
					for (i=0; i<FRAME_SIZE; i++) { frame[i] = rawBytes.remove(0); }
					
					// Parse frame
					MultiSample sample = Sensor.parse(frame);
					
					if (magnetometerIsCalibrating) {
						magnetometerIsCalibrating = false;
						magnetometerOffset = sample.magnetometer;
					}
					sample.magnetometer.applyOffset(magnetometerOffset);
					
					// Broadcast sample
					if (broadcastSamples && sample != null) { broadCastSample(sample); }
				}
			}
		} catch (IOException e) {return; }
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
		// To read ACKs
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        
        // Set sampling rate
        Log.i(TAG, "Setting sample rate");
        write(Sensor.samplingRate200HzCommand);
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
}
