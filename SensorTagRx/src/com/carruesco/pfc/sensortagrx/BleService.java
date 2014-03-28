package com.carruesco.pfc.sensortagrx;

import java.util.UUID;

import com.carruesco.pfc.sensortagrx.sensors.Accelerometer;
import com.carruesco.pfc.sensortagrx.sensors.Gyroscope;
import com.carruesco.pfc.sensortagrx.sensors.Magnetometer;
import com.carruesco.pfc.sensortagrx.sensors.Sensor;
import com.carruesco.pfc.sensortagrx.sensors.SensorSample;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;

public class BleService extends Service {
	// Log writer
    private SamplesLogger logger;
    private Time loggerStartTime = new Time();
    
	// Address of active device
	private String connectedDeviceAddress;
	
	// Block BT operations until previous one is completed
	private static volatile boolean busy = false;
	
	// Send a broadcast when the sensor starts sending data (connecting -> connected)
	private static boolean notifyIsConnected = false;
	
	// Thread to enable sensors
	Thread sensorEnablingThread;
	
	// Sensors
	private Accelerometer accelerometer = new Accelerometer();
    private Magnetometer magnetometer = new Magnetometer();
    private Gyroscope gyroscope = new Gyroscope();
    // Magnetometer calibration
	private boolean magnetometerIsCalibrating = false;
	private float[] calibrationOffset;
    
    private BluetoothGatt mBluetoothGatt;
	//private int mConnectionState = STATE_DISCONNECTED;
	
	// Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    // Broadcast sensor data?
    public static boolean broadcastData = false;
    // Broadcast types
    public final static String ACTION_DATA_AVAILABLE = "1"; //"com.carruesco.pfc.sensortagrx.ACTION_DATA_AVAILABLE";
    public final static String ACTION_CONNECTED      = "2"; //"com.carruesco.pfc.sensortagrx.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED   = "3"; //"com.carruesco.pfc.sensortagrx.ACTION_DISCONNECTED";
    // Intents
    public final static String VALUE       = "4"; //"com.carruesco.pfc.sensortagrx.VALUE";
    public final static String SENSOR_TYPE = "5"; //"com.carruesco.pfc.sensortagrx.SENSOR_TYPE";
    // Since we are now using local broadcasts instead of system wide, I do not think it's necessary
    // to include the full package name in the action.
    // I reduced the strings length to the minimum to try to increase performance.
    
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		// Stop logging
		if (logger != null) { logger.close(); }
		
		// Close communication
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
        }
		
		super.onDestroy();
	}
	
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
    	BleService getService() {
            // Return this instance of BleService so clients can call public methods
            return BleService.this;
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
    	notifyIsConnected = true;
    	
    	// Previously connected device.  Try to reconnect.
        if (connectedDeviceAddress != null && Common.deviceAddress.equals(connectedDeviceAddress) && mBluetoothGatt != null) {
            //Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                //mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = Common.mBluetoothAdapter.getRemoteDevice(Common.deviceAddress);
        if (device == null) {
            //Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        //Log.d(TAG, "Trying to create a new connection.");
        connectedDeviceAddress = Common.deviceAddress;
        //mConnectionState = STATE_CONNECTING;
        
        return true;
    }
    
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
    	// Stop worker thread (this happens when we are still enabling sensors)
    	if (sensorEnablingThread != null) {
    		if (sensorEnablingThread.isAlive()) { sensorEnablingThread.interrupt(); }
    	}
    	
    	if (Common.mBluetoothAdapter == null || mBluetoothGatt == null) { return; }
        mBluetoothGatt.disconnect();
        
        busy = false;
        
        if (logger != null) {
        	logger.close();
        	logger = null;
        }
    }
    
    private void setupSensors() {
    	// This calls are blocking and the service main thread can't be blocked
    	// or it won't receive callbacks (where the unblocks are)
    	final BleService mBleService = this;
    	sensorEnablingThread = new Thread() {
    		@Override
    		public void run() {
    			try {
    				// Enable sensors
					while(BleService.busy) { sleep(10); } accelerometer.enable(mBleService);
					while(BleService.busy) { sleep(10); } magnetometer.enable(mBleService);
					while(BleService.busy) { sleep(10); } gyroscope.enable(mBleService);
	    			// Enable notifications
					while(BleService.busy) { sleep(10); } accelerometer.enableNotifications(mBleService);
					while(BleService.busy) { sleep(10); } magnetometer.enableNotifications(mBleService);
					while(BleService.busy) { sleep(10); } gyroscope.enableNotifications(mBleService);
				} catch (InterruptedException e) {
					return;
				}
    		}
    	};
    	sensorEnablingThread.start();
    }
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
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
            	LocalBroadcastManager.getInstance(BleService.this).sendBroadcast(intent);
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
            	LocalBroadcastManager.getInstance(BleService.this).sendBroadcast(intent);
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
    };
    
    private SensorSample extractSampleAndLogIt(BluetoothGattCharacteristic characteristic) {
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
    }
    
    private void broadcastNewSample(SensorSample sample) {
    	final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
    	intent.putExtra(VALUE, SensorSample.getDataVector(sample));  
        intent.putExtra(SENSOR_TYPE, sample.getName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (Common.mBluetoothAdapter == null || mBluetoothGatt == null) { return; }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (Common.mBluetoothAdapter == null || mBluetoothGatt == null) { return; }
        
        busy = true;
        
        final UUID CCC = UUID.fromString(Sensor.CLIENT_CHARACTERISTIC_CONFIG);
        
        // Enable/disable locally
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        // Enable/disable remotely
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCC);
        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public BluetoothGattService getService(UUID uuid) {
    	return mBluetoothGatt.getService(uuid);
    }
    
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] b) {
        busy = true;
        
        characteristic.setValue(b);
        return mBluetoothGatt.writeCharacteristic(characteristic);
      }
    
    public String getAccelerometerName() { return accelerometer.getName(); }
    public String getMagnetometerName() { return magnetometer.getName(); }
    public String getGyroscopeName() { return gyroscope.getName(); }
}