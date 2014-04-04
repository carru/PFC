package com.carruesco.pfc.rempark_rx_tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.util.UUID;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class MainActivity extends Activity {
	// For the dialog to enable BT
    private static final int REQUEST_ENABLE_BT = 1;
    
	private BluetoothAdapter mBluetoothAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			finish();
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable BT.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
        	finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		
		Log.d("Main", "Starting discovery");
		mBluetoothAdapter.startDiscovery();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            Log.d("Main", "Found device " + device.getName() + " " + device.getAddress());

	            if (device.getAddress().equalsIgnoreCase("00:07:80:40:62:37")) {
	            	Log.d("Main", "Connecting");
	            	new ConnectThread(device, mBluetoothAdapter).run();
	            }
	        }
	    }
	};
}

class ConnectThread extends Thread {
	private String TAG = "ConnectThread " + Long.toString(this.getId());
	
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private BluetoothAdapter mBluetoothAdapter;
    
    // Well-known SPP UUID
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
    public ConnectThread(BluetoothDevice device, BluetoothAdapter mBluetoothAdapter) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.mBluetoothAdapter = mBluetoothAdapter;
 
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            //tmp = device.createRfcommSocketToServiceRecord(uuid);
        	tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { Log.d(TAG, "exception when creating rfcomm channel");}
        mmSocket = tmp;
    }
 
    public void run() {
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();
 
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
        	Log.d(TAG, "Attempting connection");
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
            	Log.d(TAG, "Connection failed");
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }
 
        // Do work to manage the connection (in a separate thread)
        Log.d(TAG, "Connection succeeded");
        ConnectedThread connectedThread = new ConnectedThread(mmSocket);
        connectedThread.run();
    }
 
    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}

class ConnectedThread extends Thread {
	private String TAG = "ConnectedThread " + Long.toString(this.getId());
	
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
 
    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
        	Log.d(TAG, "Opening streams");
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
 
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        
        setupSensor();
    }
    
    private void setupSensor() {
    	byte[] buffer = new byte[32];
        //int bytes = 0;
    	
    	// Commands
        byte[] samplingRate200HzCommand = {0x53, 0x41, 0x32, 0x30, 0x30, (byte) 0x99, (byte) 0x98}; // SA200
        byte[] sendThroughBTCommand = {0x42, 0x54, (byte) 0x99, (byte) 0x98}; // BT
        byte[] enableCommand = {0x4F, 0x4E, (byte) 0x99, (byte) 0x98}; // ON
        
        // Set sampling rate
        Log.i(TAG, "Setting sample rate");
		write(samplingRate200HzCommand);
        // Read ACK
		read(buffer);
		//Log.d(TAG,String.valueOf(isAck(buffer)));
		
		//Log.d(TAG, "Read " + bytes + " bytes");
       
        // Set to send through BT
        Log.i(TAG, "Setting to send through BT");
        write(sendThroughBTCommand);
        // Read ACK
     	read(buffer);
     	//Log.d(TAG, "Read " + bytes + " bytes");
        
        // Enable
        Log.i(TAG, "Enabling sensor");
        write(enableCommand);
        // Read ACK
     	read(buffer);
     	//Log.d(TAG, "Read " + bytes + " bytes");
    }
 
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
 
        Log.d(TAG, "Reading");
        // Keep listening to the InputStream until an exception occurs
        /*while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                Log.d(TAG, "Read " + bytes + " bytes");
                
                if (bytes > 0) {
                	//Log.d(TAG, "Read: " + buffer);
                }
                
                // Send the obtained bytes to the UI activity
                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                break;
            }
        }*/
    }

    // DOES NOT WORK PROPERLY
    private boolean isAck(byte[] data) {
    	byte[] ackCommand = {0x41, 0x43, 0x4B, (byte) 0x99, (byte) 0x98}; // ACK
    	
    	for (int i=0; i<data.length; i++) {
    		if (data[i] != ackCommand[i]) { return false; }
    	}
    	
    	return true;
    }
    
    private int read(byte[] buffer) {
    	int bytes;
    	
    	try {
			bytes = mmInStream.read(buffer);
		} catch (IOException e) { return 0; }
    	
		return bytes;
    }
    
    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}