package com.carruesco.pfc.rempark_rx.fragments;

import java.util.ArrayList;

import com.carruesco.pfc.rempark_rx.BTService;
import com.carruesco.pfc.rempark_rx.Common;
import com.carruesco.pfc.rempark_rx.FavouritesList;
import com.carruesco.pfc.rempark_rx.R;
import com.carruesco.pfc.rempark_rx.adapters.LeDeviceListAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
    
    // For the dialog to enable BT
    private static final int REQUEST_ENABLE_BT = 1;
    
    // Favourite devices list
    private ArrayList<String> list;
    private boolean listHasBeenModified = false;
    
	// Empty constructor required for fragment subclasses
	public ScanFragment() {}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Common.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (Common.mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			getActivity().finish();
		}
    }
	
	private void initiateConnection(BluetoothDevice device) {
		if (device == null) return;
        
        // Stop scanning
        stopScan();
        
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
	        		if (Common.mBluetoothAdapter.isDiscovering()) {
	        			// Stop scanning
	        			stopScan();
	        		} else {
	        			// Clear device list
	        			mLeDeviceListAdapter.clear();
	        			mLeDeviceListAdapter.notifyDataSetChanged();
	        			
	        			// Start scanning
	        			if (Common.isConnected) Common.disconnect();
	        			startScan();
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
     		if (!bluetoothWasOff) { startScan(); }
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
        if (Common.mBluetoothAdapter.isDiscovering()) { stopScan(); }
        unRegisterReciever();

        // Save favourites list if it has been modified
        if (listHasBeenModified) { FavouritesList.save(Common.sharedPref, list); }
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
     
    private void startScan() {
    	Common.disconnect();
    	Common.mBluetoothAdapter.startDiscovery();
    	
    	// Change UI
        setUi(SCANNING);
    }
    
    private void stopScan() {
        Common.mBluetoothAdapter.cancelDiscovery();
    }
    
    private void registerReceiver() {
    	LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
    			makeGattUpdateIntentFilter());
    	getActivity().registerReceiver(mBtReceiver, makeBtIntentFilter());
    }
    
    private void unRegisterReciever() {
    	LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    	getActivity().unregisterReceiver(mBtReceiver);
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BTService.ACTION_DISCONNECTED);
        intentFilter.addAction(BTService.ACTION_CONNECTED);
        return intentFilter;
    }
    
    private static IntentFilter makeBtIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return intentFilter;
    }
    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    		  if (intent.getAction().equals(BTService.ACTION_CONNECTED)) {
    			  setUi(CONNECTED);
    		  }
    		  else { // ACTION_DISCONNECTED
    			  setUi(DISCONNECTED);
    		  }
    	  }
    };
    
    // Create a BroadcastReceiver for BT scanner
 	private final BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			// When discovery finds a device
			if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				// Get the BluetoothDevice object from the Intent
				final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				final short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mLeDeviceListAdapter.addDevice(device, (int) rssi);
						mLeDeviceListAdapter.notifyDataSetChanged();

						// Is favourite system enabled?
						if (Common.sharedPref.getBoolean("key_favourites_system", false)) {
							// If device is on the favourites list, connect
							if (isFavourite(device)) {
								initiateConnection(device);
								Toast.makeText(getActivity(),R.string.connected_to_favourite,Toast.LENGTH_LONG).show();
							}
						}
					}
				});
			}
			else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				if (!mConnectionState.getText().equals(getString(R.string.connected))) {
					setUi(DISCONNECTED);
				}
			}
		}
	};
}