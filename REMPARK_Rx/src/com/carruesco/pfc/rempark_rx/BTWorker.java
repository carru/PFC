package com.carruesco.pfc.rempark_rx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BTWorker extends Thread {
	private String TAG = "BTWorker";
	
	private final int READ_BUFFER_SIZE = 512;
	
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
				Log.d(TAG, "Read: " + bytesRead + " bytes");
				
				// Add read bytes to list
				for (i=0; i<bytesRead; i++) { rawBytes.add(readBuffer[i]); }
				Log.d(TAG, "rawBytes: " + rawBytes.size());
				
				// Do we have a full frame?
				if (rawBytes.size()>=FRAME_SIZE) {
					// Get frame from ArrayList
					for (i=0; i<FRAME_SIZE; i++) { frame[i] = rawBytes.remove(0); }
					
					// Parse frame
					parse(frame);
				}
			}
		} catch (IOException e) {return; }
	}
	
	private boolean frameIsValid(byte[] frame) {
		// Check size
		if (frame.length != 32) { return false; }
		
		// Check start sequence
		if (frame[0] != 0x11) { return false; }
		if (frame[1] != 0x22) { return false; }
		if (frame[2] != 0x33) { return false; }
		if (frame[3] != 0x44) { return false; }
		
		return true;
	}
	
	private void parse(byte[] frame) {
		if (!frameIsValid(frame)) {
			Log.e(TAG, "Frame invalid!");
			Log.e(TAG, Integer.toHexString(frame[0]) + 
					Integer.toHexString(frame[1]) + 
					Integer.toHexString(frame[2]) + 
					Integer.toHexString(frame[3]));
			return;
		}
		
		// Accelerometer
		ByteBuffer bBuffer = ByteBuffer.wrap(frame, 4, 28);
		double Ax = bBuffer.getShort() * 2.9412;
		double Ay = bBuffer.getShort() * 2.9412;
		double Az = bBuffer.getShort() * 2.9412;
		//Log.i(TAG, "Ax: " + Ax + "Ay: " + Ay + "Az: " + Az);
		
		// Gyroscope
		double Gx = ((bBuffer.getShort() * 3.3/4096) - 1.35) * 2000;
		double Gy = ((bBuffer.getShort() * 3.3/4096) - 1.35) * 2000;
		double Gz = ((bBuffer.getShort() * 3.3/4096) - 1.35) * 2000;
		
		// Magnetometer
		double Mx = bBuffer.getShort() * 3.3/4096;
		double My = bBuffer.getShort() * 3.3/4096;
		double Mz = bBuffer.getShort() * 3.3/4096;
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
        
    	// Commands
        byte[] samplingRate200HzCommand = {0x53, 0x41, 0x32, 0x30, 0x30, (byte) 0x99, (byte) 0x98}; // SA200
        byte[] samplingRate10HzCommand = {0x53, 0x41, 0x30, 0x31, 0x30, (byte) 0x99, (byte) 0x98}; // SA010
        byte[] sendThroughBTCommand = {0x42, 0x54, (byte) 0x99, (byte) 0x98}; // BT
        byte[] enableCommand = {0x4F, 0x4E, (byte) 0x99, (byte) 0x98}; // ON
        
        // Set sampling rate
        Log.i(TAG, "Setting sample rate");
        write(samplingRate10HzCommand);
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
