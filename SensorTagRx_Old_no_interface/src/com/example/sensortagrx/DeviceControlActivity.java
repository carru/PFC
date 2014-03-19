package com.example.sensortagrx;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    // Log writer
    SamplesLogger logger;
    
    // Sensors
    private Accelerometer accelerometer = new Accelerometer();
    private Magnetometer magnetometer = new Magnetometer();
    private Gyroscope gyroscope = new Gyroscope();
    
    // Received sample counters
    private int accelerometerCounter;
    private int magnetometerCounter;
    private int gyroscopeCounter;
    
    // Magnetometer calibration (offset)
    private boolean isCalibrating = false;
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetZ = 0;
    
    private boolean accelerometerActive = false;
    private boolean magnetometerActive = false;
    private boolean gyroscopeActive = false;
        
    // UI elements
    private TextView mConnectionState;
    private TextView accelerometerDataField;
    private TextView magnetometerDataField;
    private TextView gyroscopeDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED:    		connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: 		disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: 			received data from the device.  This can be a result of read
    //                        			or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	setupSensors();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                getData(intent);
            }
        }
    };

    private final void setupSensors() {
    	accelerometerActive = false;
    	magnetometerActive = false;
    	gyroscopeActive = false;
    	
    	// FOR DEBUG. REMOVE THIS!!!!
    	accelerometerActive = true;
    	magnetometerActive = true;
    	gyroscopeActive = true;
    	
    	// Debug, to avoid cluttering
    	SamplesLogger.deleteAllLogs(this);
    	
    	// Prepare log writer
    	logger = new SamplesLogger(this);
    	if (!logger.isReady()) {
    		Toast.makeText(this, R.string.error_fs_not_writable, Toast.LENGTH_LONG).show();
    	}
    	
    	accelerometerCounter = 0;
        magnetometerCounter = 0;
        gyroscopeCounter = 0;
    	
    	// Enable sensors
        //updateConnectionState(R.string.enabling_sensors);
    	accelerometer.enable(mBluetoothLeService);
    	magnetometer.enable(mBluetoothLeService);
    	gyroscope.enable(mBluetoothLeService);

  		// Enable notifications
    	//updateConnectionState(R.string.enabling_notifications);
    	accelerometer.enableNotifications(mBluetoothLeService);
    	magnetometer.enableNotifications(mBluetoothLeService);
    	gyroscope.enableNotifications(mBluetoothLeService);
    	
    	// Set period
    	// By default: accelerometer: 1 second
    	//			   magnetometer:  2 seconds
    	//			   gyroscope:	  1 second
    	// I have changed the default values (including gyroscope) in the SensorTag firmware
    	//accelerometer.setPeriod(mBluetoothLeService, Sensor.MIN_PERIOD);
    	//magnetometer.setPeriod(mBluetoothLeService, Sensor.MIN_PERIOD);
    	//magnetometer.setPeriod(mBluetoothLeService, 100); // 1 second
    }
    
    private void clearUI() {
    	accelerometerDataField.setText(R.string.no_data);
    	magnetometerDataField.setText(R.string.no_data);
    	gyroscopeDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_data);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Set up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        accelerometerDataField = (TextView) findViewById(R.id.accelerometer_data);
        magnetometerDataField = (TextView) findViewById(R.id.magnetometer_data);
        gyroscopeDataField = (TextView) findViewById(R.id.gyroscope_data);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        // Button listener
        final Button calibrate_button = (Button) findViewById(R.id.calibrate_button);
        calibrate_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isCalibrating = true;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        logger.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                logger.close();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void getData(Intent intent) {
    	float[] data = intent.getFloatArrayExtra(BluetoothLeService.EXTRA_DATA);
        String sensorName = intent.getStringExtra(BluetoothLeService.SENSOR_TYPE);
        SensorSample sample = new SensorSample();
        
        if (sensorName.equals(accelerometer.getName())) {
        	accelerometerActive = true;
        	accelerometerCounter++;
        	
        	sample = convertData(data);
        	
        	if (accelerometerActive && magnetometerActive && gyroscopeActive) {
        		logger.writeAccelerometerSample(sample);
        		displayAccelerometerData(sample);
        	}
        } else if (sensorName.equals(magnetometer.getName())) {
        	magnetometerActive = true;
        	magnetometerCounter++;
        	
        	// Calibration
        	if (isCalibrating) {
        		offsetX = data[0];
        		offsetY = data[1];
        		offsetZ = data[2];
        		isCalibrating = false;
        	}
        		data[0] = data[0] - offsetX;
        		data[1] = data[1] - offsetY;
        		data[2] = data[2] - offsetZ;
        	
        		sample = convertData(data);
        		
        	if (accelerometerActive && magnetometerActive && gyroscopeActive) {
        		logger.writeMagnetometerSample(sample);
        		displayMagnetometerData(sample);
        	}
        } else if (sensorName.equals(gyroscope.getName())) {
        	gyroscopeActive = true;
        	gyroscopeCounter++;
        	
        	sample = convertData(data);
        	
        	if (accelerometerActive && magnetometerActive && gyroscopeActive) {
        		logger.writeGyroscopeSample(sample);
        		displayGyroscopeData(sample);
        	}
        }
    }
    
    private SensorSample convertData(float[] data) {
    	return new SensorSample(data[0], data[1], data[2]);
    }
    
    private void displayAccelerometerData(SensorSample data) {
    	accelerometerDataField.setText("x: " + Float.toString(data.getX()) + " g\n" +
				   "y: " + Float.toString(data.getY()) + " g\n" +
				   "z: " + Float.toString(data.getZ()) + " g\n" + accelerometerCounter);
    }
    
    private void displayMagnetometerData(SensorSample data) {
    	magnetometerDataField.setText("x: " + Float.toString(data.getX()) + " uT\n" +
				  "y: " + Float.toString(data.getY()) + " uT\n" +
				  "z: " + Float.toString(data.getZ()) + " uT\n" + Integer.toString(accelerometerCounter-magnetometerCounter));
    }
    
    private void displayGyroscopeData(SensorSample data) {
    	gyroscopeDataField.setText("x: " + Float.toString(data.getX()) + " deg/s\n" +
				   "y: " + Float.toString(data.getY()) + " deg/s\n" +
				   "z: " + Float.toString(data.getZ()) + " deg/s\n" + Integer.toString(accelerometerCounter-gyroscopeCounter));
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
