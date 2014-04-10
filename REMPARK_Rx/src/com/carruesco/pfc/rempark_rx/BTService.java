package com.carruesco.pfc.rempark_rx;

import java.io.IOException;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class BTService extends Service {
	//private String TAG = "BTService";
	
	private BluetoothSocket mmSocket;
	
	// Worker thread to manage BT connection and logger
	private BTWorker worker;
	
	// Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    // Broadcast types
    public final static String ACTION_DATA_AVAILABLE = "1";
    public final static String ACTION_CONNECTED      = "2";
    public final static String ACTION_DISCONNECTED   = "3";
    // Intents
    public final static String VALUE       = "4";
    //public final static String SENSOR_TYPE = "5";
    
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		disconnect();
		
		super.onDestroy();
	}
	
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
    	BTService getService() {
            // Return this instance of BleService so clients can call public methods
            return BTService.this;
        }
    }
    
    public boolean startLogger() {
    	if (worker == null) { return false; }
    	return worker.startLogger();
    }
    
    public boolean startLogger(String fileName) {
    	if (worker == null) { return false; }
    	return worker.startLogger(fileName);
    }
    
    public void stopLogger() {
    	if (worker != null) { worker.stopLogger(); }
    }
    
    public boolean isLogging() {
    	if (worker == null) { return false; }
    	return worker.isLogging();
    }
    
    public String getLoggerFileName() {
    	if (worker == null) { return null; }
    	return worker.getLoggerFileName();
    }
    
    public int getLoggerNumberOfSamples() {
    	if (worker == null) { return -1; }
    	return worker.getLoggerNumberOfSamples();
    }
    
    public Time getLoggerStartTime() {
    	if (worker == null) { return null; }
    	return worker.getLoggerStartTime();
    }
    
    public boolean connect() {
        final BluetoothDevice device = Common.mBluetoothAdapter.getRemoteDevice(Common.deviceAddress);
        // Well-known SPP UUID
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        
        // Open socket with the device
        try {
        	mmSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { 
        	Log.e("Service", "Exception when opening socket");
        	return false;
        }
        
        worker = new BTWorker(this, mmSocket);
        worker.start();
        
        return true;
    }
    
    public void disconnect() {
    	// Stop worker thread
    	if (worker != null) { worker.interrupt(); }
    	
    	// Close socket
    	closeSocket();
        
    	// Close logger if active
        stopLogger();
    }
    
    private void closeSocket() {
    	if (mmSocket != null) {
	    	try {
				mmSocket.close();
				Log.i("Service", "Socket closed");
			} catch (IOException e) { }
    	}
    }
}