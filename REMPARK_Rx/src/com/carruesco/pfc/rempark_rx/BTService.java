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
	
	// Worker thread to manage BT connection
	private BTWorker worker;
	
	// Log writer
    private SamplesLogger logger;
    private Time loggerStartTime = new Time();
	
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
    	// Prepare log writer
    	logger = new SamplesLogger(this);
    	if (!logger.isReady()) { return false; }
    	loggerStartTime.setToNow();
    	return true;
    }
    
    public boolean startLogger(String fileName) {
    	// Prepare log writer
    	logger = new SamplesLogger(this, fileName);
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
    
    public void calibrateMagnetometer() {
    	//magnetometerIsCalibrating = true;
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
        if (logger != null) {
        	logger.close();
        	logger = null;
        }
    }
    
    private void closeSocket() {
    	if (mmSocket != null) {
	    	try {
				mmSocket.close();
				Log.i("Service", "Socket closed");
			} catch (IOException e) { }
    	}
    }
    
    /*private SensorSample extractSampleAndLogIt(BluetoothGattCharacteristic characteristic) {
    	// From which sensor?
        if (characteristic.getUuid().toString().equalsIgnoreCase(accelerometer.getDataUUID())) {
        	// Accelerometer
        	accelerometer.isSendingData = true;
        	
            float[] data = accelerometer.parse(characteristic);
            SensorSample sample = new SensorSample(data, accelerometer.getName());
            
            // Are we logging samples?
            if (logger != null) {
            	// Log samples only if all sensors are ready
            	boolean sensorsAreAllReady = accelerometer.isSendingData &&
            							 magnetometer.isSendingData &&
            							 gyroscope.isSendingData;
            	if (logger.isReady() && sensorsAreAllReady) { logger.writeAccelerometerSample(sample); }
            }
            
            return sample;
        }
        else if (characteristic.getUuid().toString().equalsIgnoreCase(magnetometer.getDataUUID())) {
        	// Magnetometer
        	magnetometer.isSendingData = true;
        	
        	float[] data = magnetometer.parse(characteristic);
        	if (magnetometerIsCalibrating) {
        		calibrationOffset[0] = data[0];
        		calibrationOffset[1] = data[1];
        		calibrationOffset[2] = data[2];
        		magnetometerIsCalibrating = false;
        	}
        	data[0] = data[0] - calibrationOffset[0];
    		data[1] = data[1] - calibrationOffset[1];
    		data[2] = data[2] - calibrationOffset[2];
        	SensorSample sample = new SensorSample(data, magnetometer.getName());
        	
        	// Are we logging samples?
            if (logger != null) {
            	// Log samples only if all sensors are ready
            	boolean sensorsAreAllReady = accelerometer.isSendingData &&
            							 magnetometer.isSendingData &&
            							 gyroscope.isSendingData;
            	if (logger.isReady() && sensorsAreAllReady) { logger.writeMagnetometerSample(sample); }
            }
        	
        	return sample;
        }
        else if (characteristic.getUuid().toString().equalsIgnoreCase(gyroscope.getDataUUID())) {
        	// Gyroscope
        	gyroscope.isSendingData = true;
        	
        	float[] data = gyroscope.parse(characteristic);
        	SensorSample sample = new SensorSample(data, gyroscope.getName());
        	
        	// Are we logging samples?
            if (logger != null) {
            	// Log samples only if all sensors are ready
            	boolean sensorsAreAllReady = accelerometer.isSendingData &&
            							 magnetometer.isSendingData &&
            							 gyroscope.isSendingData;
            	if (logger.isReady() && sensorsAreAllReady) { logger.writeGyroscopeSample(sample); }
            }
        	
        	return sample;
        }
        
		return null;
    }*/
    
    /*private void broadcastNewSample(SensorSample sample) {
    	final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
    	intent.putExtra(VALUE, SensorSample.getDataVector(sample));  
        intent.putExtra(SENSOR_TYPE, sample.getName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    public String getAccelerometerName() { return accelerometer; }
    public String getMagnetometerName() { return magnetometer; }
    public String getGyroscopeName() { return gyroscope; }*/
}