package com.carruesco.pfc.rempark_rx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BTWorker extends Thread {
	private String TAG = "BTWorker";
	
	// Context to send broadcasts
	Context context;
	
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
        
        // Everything OK, sensor is now connected and sending data
        Log.i(TAG, "Setup complete. Sensor should now start sending data");
        updateConnectionState(CONNECTED);
        
		super.run();
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
        byte[] buffer = new byte[1024];
        
    	// Commands
        byte[] samplingRate200HzCommand = {0x53, 0x41, 0x32, 0x30, 0x30, (byte) 0x99, (byte) 0x98}; // SA200
        byte[] sendThroughBTCommand = {0x42, 0x54, (byte) 0x99, (byte) 0x98}; // BT
        byte[] enableCommand = {0x4F, 0x4E, (byte) 0x99, (byte) 0x98}; // ON
        
        // Set sampling rate
        Log.i(TAG, "Setting sample rate");
        write(samplingRate200HzCommand);
        // Read ACK
		read(buffer);
        if (!isACK(buffer)) {
        	Log.e(TAG, "Error setting sample rate");
        	return false;
        }
		
        // Set to send through BT
        Log.i(TAG, "Setting to send through BT");
        write(sendThroughBTCommand);
        // Read ACK
        read(buffer);
        if (!isACK(buffer)) {
        	Log.e(TAG, "Error setting to send through BT");
        	return false;
        }
        
        // Enable
        Log.i(TAG, "Enabling sensor");
        write(enableCommand);
        // Read ACK
        read(buffer);
        if (!isACK(buffer)) {
        	Log.e(TAG, "Error enabling sensor");
        	return false;
        }
		
		return true;
	}
	
	private boolean isACK(byte[] message) {
		byte[] ackCommand = {0x41, 0x43, 0x4B, (byte) 0x99, (byte) 0x98}; // ACK
		
		for (int i=0; i<message.length; i++) {
			if (i == ackCommand.length) { return true; }
			if (message[i] != ackCommand[i]) { return false; }
		}
		
		// Should never get here
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
