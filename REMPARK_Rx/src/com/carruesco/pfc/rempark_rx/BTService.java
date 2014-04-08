package com.carruesco.pfc.rempark_rx;

import java.io.IOException;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

public class BTService extends Service {
	private BluetoothSocket mmSocket;
	
	// Log writer
    private SamplesLogger logger;
    private Time loggerStartTime = new Time();
	
	// Send a broadcast when the sensor starts sending data (connecting -> connected)
	//private static boolean notifyIsConnected = false;
	
	// Sensors
	private String accelerometer = "A";
	private String magnetometer = "M";
	private String gyroscope = "G";
	/*private Accelerometer accelerometer = new Accelerometer();
    private Magnetometer magnetometer = new Magnetometer();
    private Gyroscope gyroscope = new Gyroscope();*/
    // Magnetometer calibration
	private boolean magnetometerIsCalibrating = false;
	private float[] calibrationOffset;
	
	// Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    // Broadcast sensor data?
    public static boolean broadcastData = false;
    // Broadcast types
    public final static String ACTION_DATA_AVAILABLE = "1";
    public final static String ACTION_CONNECTED      = "2";
    public final static String ACTION_DISCONNECTED   = "3";
    // Intents
    public final static String VALUE       = "4";
    public final static String SENSOR_TYPE = "5";
    
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		// Stop logging
		if (logger != null) { logger.close(); }
		
		// Close communication
		closeSocket();
		
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
    	magnetometerIsCalibrating = true;
    }
    
    private void initializeCalibrationOffset() {
    	calibrationOffset = new float[]{0, 0, 0};
    }
    
    public boolean connect() {
        final BluetoothDevice device = Common.mBluetoothAdapter.getRemoteDevice(Common.deviceAddress);
        // Well-known SPP UUID
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        
        // Open socket with the device
        try {
        	mmSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { 
        	Log.i("Service", "Exception when opening socket");
        	return false;
        }
        
        //TODO
        // Start a new thread to handle the setup procedure
        
        Log.i("Service", "Socket opened");
        return true;
    }
    
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    /*private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Attempts to discover services after successful connection.
            	mBluetoothGatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	Common.isConnected = false;
            	Common.isConnecting = false;
            	disconnect();
            	Intent intent = new Intent(ACTION_DISCONNECTED);
            	LocalBroadcastManager.getInstance(BTService.this).sendBroadcast(intent);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	// Services discovery completed
            	initializeCalibrationOffset();
            	setupSensors();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Notify that the device is connected and ready (sending data)
        	if (notifyIsConnected) {
            	Common.isConnected = true;
            	Common.isConnecting = false;
            	Intent intent = new Intent(ACTION_CONNECTED);
            	LocalBroadcastManager.getInstance(BTService.this).sendBroadcast(intent);
            	notifyIsConnected = false;
            }
            
            SensorSample sample = extractSampleAndLogIt(characteristic);
            
            if (broadcastData) { broadcastNewSample(sample); }
        }
        
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	// Write operation completed
        	busy = false;
        }
        
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        	// Write operation completed
        	busy = false;
        }
    };*/
    
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
    
    private void broadcastNewSample(SensorSample sample) {
    	final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
    	intent.putExtra(VALUE, SensorSample.getDataVector(sample));  
        intent.putExtra(SENSOR_TYPE, sample.getName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    public String getAccelerometerName() { return accelerometer; }
    public String getMagnetometerName() { return magnetometer; }
    public String getGyroscopeName() { return gyroscope; }
}