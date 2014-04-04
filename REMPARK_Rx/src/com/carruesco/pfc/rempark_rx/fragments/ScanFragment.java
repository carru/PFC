package com.carruesco.pfc.rempark_rx.fragments;

import java.util.ArrayList;

import com.carruesco.pfc.rempark_rx.BleService;
import com.carruesco.pfc.rempark_rx.Common;
import com.carruesco.pfc.rempark_rx.FavouritesList;
import com.carruesco.pfc.rempark_rx.R;
import com.carruesco.pfc.rempark_rx.adapters.LeDeviceListAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ScanFragment extends Fragment {
	// UI elements
    private TextView mConnectionState;
    private TextView mDeviceName;
    private TextView mDeviceAddress;
    private Button scanButton;
    private Button disconnectButton;
    private ListView mListView;
    private LinearLayout scanCard;
    private LinearLayout statusCard;
    
    private static final int CONNECTED = 1;
    private static final int DISCONNECTED = 2;
    private static final int SCANNING = 3;
    private static final int CONNECTING = 4;
    
    private LeDeviceListAdapter mLeDeviceListAdapter;
	private boolean mScanning;
    private Thread stopScanThread;
    
    // For the dialog to enable BT
    private static final int REQUEST_ENABLE_BT = 1;
    
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    
    // Favourite devices list
    private ArrayList<String> list;
    private boolean listHasBeenModified = false;
    
	// Empty constructor required for fragment subclasses
	public ScanFragment() {}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        if (Common.mBluetoothAdapter == null) {
        	final BluetoothManager bluetoothManager =
                    (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            Common.mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        // Checks if Bluetooth is supported on the device.
        if (Common.mBluetoothAdapter == null) {
            getActivity().finish();
            return;
        }
    }
	
	private void initiateConnection(BluetoothDevice device) {
		if (device == null) return;
        
        // Stop scanning
        scanLeDevice(false);
        
        // Disconnect from current device
        if (Common.isConnected) { Common.disconnect(); }
        
        // Get selected device properties
        Common.deviceName = device.getName();
        Common.deviceAddress = device.getAddress();
        
        // Connect
        if (Common.connect()) {
        	mLeDeviceListAdapter.clear();
        	mLeDeviceListAdapter.notifyDataSetChanged();
        	
        	// Update UI
        	setUi(CONNECTING);
        } else {
        	Toast.makeText(getActivity(), R.string.connect_failed, Toast.LENGTH_LONG).show();
        }
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.scan_fragment, container, false);
		
		scanCard = (LinearLayout) rootView.findViewById(R.id.ScanCard);
		statusCard = (LinearLayout) rootView.findViewById(R.id.StatusCard);
		
		mConnectionState = (TextView) rootView.findViewById(R.id.connection_state);
		mDeviceName = (TextView) rootView.findViewById(R.id.device_name);
		mDeviceAddress = (TextView) rootView.findViewById(R.id.device_address);
		
		mListView = (ListView) rootView.findViewById(R.id.device_list);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				initiateConnection(mLeDeviceListAdapter.getDevice(position));
				
				// Ask to include device in favourites
				if (Common.sharedPref.getBoolean("key_favourites_system", false)) {
					askToIncludeInFavourites();
				}
			}
		});
		
		scanButton = (Button) rootView.findViewById(R.id.scan_button);
		scanButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
	        		if (mScanning) {
	        			// Stop scanning
	        			scanLeDevice(false);
	        		} else {
	        			// Clear device list
	        			mLeDeviceListAdapter.clear();
	        			mLeDeviceListAdapter.notifyDataSetChanged();
	        			
	        			// Start scanning
	        			if (Common.isConnected) Common.disconnect();
	        			scanLeDevice(true);
	        		}
		    }
		});
		
		disconnectButton = (Button) rootView.findViewById(R.id.disconnect_button);
		disconnectButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	Common.disconnect();
		    }
		});
		
		return rootView;
    }
	
	private void askToIncludeInFavourites() {
		new AlertDialog.Builder(getActivity())
	    .setMessage(getString(R.string.add_device_to_favourites))
	    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	// Add device to favourites
	        	list.add(Common.deviceAddress);
	        	listHasBeenModified = true;
	        }
	    }).setNegativeButton(getString(R.string.no), null).show();
	}
	
	@Override
	public void onResume() {
        super.onResume();
        registerReceiver();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        boolean bluetoothWasOff = false;
        if (!Common.mBluetoothAdapter.isEnabled()) {
            if (!Common.mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                bluetoothWasOff = true;
            }
        }

        // Get list of favourites
        if (Common.sharedPref.getBoolean("key_favourites_system", false)) {
        	list = FavouritesList.getList(Common.sharedPref);
        }
        
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(getActivity());
        mListView.setAdapter(mLeDeviceListAdapter);
        
        // Recover state
        if (Common.isConnected) { setUi(CONNECTED); }
        
        // Perform startup scan
        if (Common.scanOnStartup) {
     		Common.scanOnStartup = false;
     		// Don't scan if we had to turn on BT (prevent black screen)  
     		if (!bluetoothWasOff) { scanButton.callOnClick(); }
     	}
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable BT.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
        	getActivity().finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
	public void onPause() {
        super.onPause();
        if (mScanning) { scanLeDevice(false); }
        unRegisterReciever();

        // Save favourites list if it has been modified
        if (listHasBeenModified) { FavouritesList.save(Common.sharedPref, list); }
    }
    
    private void scanLeDevice(final boolean enable) {
        if (enable) {
        	// This thread will stop the scanner after SCAN_PERIOD milliseconds
        	stopScanThread = new Thread() {
        		@Override
        		public void run() {
        			try {
        				sleep(SCAN_PERIOD);
        			} catch (InterruptedException e) {
    					return;
    				}
        			mScanning = false;
	                Common.mBluetoothAdapter.stopLeScan(mLeScanCallback);
	                    
	                // Change UI unless already connected (user connected to a device while scanning)
	                // This thread cannot change the UI, so send a broadcast to order it
	                if (!mConnectionState.getText().equals(getString(R.string.connected))) {
	                    Intent intent = new Intent(BleService.ACTION_DISCONNECTED);
	                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
	                }
        		}
        	};
        	stopScanThread.start();

            mScanning = true;
            Common.mBluetoothAdapter.startLeScan(mLeScanCallback);
            Common.disconnect();
            
            // Change UI
            setUi(SCANNING);
        } else {
        	// Stop the thread so it doesn't interfere with future scans
        	if (stopScanThread != null) { stopScanThread.interrupt(); }
        	
            mScanning = false;
            Common.mBluetoothAdapter.stopLeScan(mLeScanCallback);

            // Change UI
            setUi(DISCONNECTED);
        }
    }
    
    private void setUi(int type) {
    	switch (type) {
    		case CONNECTED:
    			mConnectionState.setText(R.string.connected);
        		mDeviceName.setText(Common.deviceName);
        		mDeviceAddress.setText(Common.deviceAddress);
        		statusCard.setBackgroundResource(R.drawable.card_greenborder);
        		scanButton.setText(R.string.scan);
        		disconnectButton.setVisibility(Button.VISIBLE);
        		scanCard.setBackgroundResource(R.drawable.card_no_border);
    			break;
    		case DISCONNECTED:
    			mConnectionState.setText(R.string.disconnected);
    			mDeviceName.setText("");
    			mDeviceAddress.setText("");
        		statusCard.setBackgroundResource(R.drawable.card_no_border);
        		scanButton.setText(R.string.scan);
        		disconnectButton.setVisibility(Button.GONE);
        		scanCard.setBackgroundResource(R.drawable.card_no_border);
    			break;
    		case SCANNING:
    			mConnectionState.setText(R.string.scanning);
    			mDeviceName.setText("");
        		mDeviceAddress.setText("");
        		statusCard.setBackgroundResource(R.drawable.card_no_border);
        		scanButton.setText(R.string.stop);
        		disconnectButton.setVisibility(Button.GONE);
                scanCard.setBackgroundResource(R.drawable.card_goldborder);
    			break;
    		case CONNECTING:
    			mConnectionState.setText(R.string.connecting);
    			mDeviceName.setText(Common.deviceName);
        		mDeviceAddress.setText(Common.deviceAddress);
        		statusCard.setBackgroundResource(R.drawable.card_goldborder);
        		scanButton.setText(R.string.scan);
        		disconnectButton.setVisibility(Button.VISIBLE);
        		scanCard.setBackgroundResource(R.drawable.card_no_border);
    			break;
    	}
    }
    
    private boolean isFavourite(BluetoothDevice device) {
    	if (list == null) { return false; }
    	return list.contains(device.getAddress());
    }
    
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
        	getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	mLeDeviceListAdapter.addDevice(device, rssi);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    
                    // Is favourite system enabled?
                    if (Common.sharedPref.getBoolean("key_favourites_system", false)) {
                    	// If device is on the favourites list, connect
                    	if (isFavourite(device)) {
                    		initiateConnection(device);
                    		Toast.makeText(getActivity(), R.string.connected_to_favourite, Toast.LENGTH_LONG).show();
                    	}
                    }
                }
            });
        }
    };
    
    private void registerReceiver() {
    	LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
    			makeGattUpdateIntentFilter());
    }
    
    private void unRegisterReciever() {
    	LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_CONNECTED);
        return intentFilter;
    }
    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    		  if (intent.getAction().equals(BleService.ACTION_CONNECTED)) {
    			  setUi(CONNECTED);
    		  }
    		  else { // ACTION_DISCONNECTED
    			  setUi(DISCONNECTED);
    		  }
    	  }
    };
}