package com.carruesco.pfc.sensortagrx.fragments;

import java.util.concurrent.TimeUnit;

import com.carruesco.pfc.sensortagrx.BleService;
import com.carruesco.pfc.sensortagrx.Common;
import com.carruesco.pfc.sensortagrx.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CaptureFragment extends Fragment {
	// UI elements
    private TextView mConnectionState;
    private TextView mDeviceName;
    private TextView mDeviceAddress;
    private TextView mLoggerStatus;
    private Button logButton;
    private LinearLayout statusCard;
    private TextView mFileName;
    private TextView mLogStartTime;
    private TextView mLogRuntime;
    private TextView mLogSamples;
    private TextView mFileLocation;
    
	private static final int CONNECTED = 1;
	private static final int CONNECTING = 2;
	private static final int DISCONNECTED = 3;
	
	// Thread to refresh runtime and number of samples written
	private Thread refresher;
	private int REFRESH_PERIOD = 1000; // milliseconds
	
	//private
	
	// Empty constructor required for fragment subclasses
	public CaptureFragment() {}
	
	private void getUiReferences(View v) {
		statusCard = (LinearLayout) v.findViewById(R.id.capture_status_card);
		
		mConnectionState = (TextView) v.findViewById(R.id.capture_connection_state);
		mDeviceName = (TextView) v.findViewById(R.id.capture_device_name);
		mDeviceAddress = (TextView) v.findViewById(R.id.capture_device_address);
		mLoggerStatus = (TextView) v.findViewById(R.id.capture_logger_status);
		mFileName = (TextView) v.findViewById(R.id.capture_log_filename);
		mLogStartTime = (TextView) v.findViewById(R.id.capture_start_time);
		mLogRuntime = (TextView) v.findViewById(R.id.capture_runtime);
		mLogSamples = (TextView) v.findViewById(R.id.capture_number_of_samples);
		mFileLocation = (TextView) v.findViewById(R.id.capture_TextView9);
		
		logButton = (Button) v.findViewById(R.id.capture_logger_button);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.capture_fragment, container, false);
		
		getUiReferences(rootView);
		
		mFileLocation.setText(getString(R.string.file_location) + " (" + getActivity().getExternalFilesDir(null) + ")");
		
		logButton.setOnClickListener(new View.OnClickListener() {
		    @SuppressLint("DefaultLocale")
			public void onClick(View v) {
		    	if (Common.mService.isLogging()) {
		    		stopRefreshingInfo();
		    		
		    		Common.mService.stopLogger();
		    		logButton.setText(R.string.start_log);
			    	mLoggerStatus.setText(R.string.not_logging);
		    	}
		    	else {
		    		// Don't start logger if we're not connected
		    		if (!Common.isConnected) {
		    			Toast.makeText(getActivity(), R.string.error_not_connected, Toast.LENGTH_LONG).show();
		    			return;
		    		}
		    		
		    		// Ask user for file name
		    		final EditText input = new EditText(getActivity());
		    		new AlertDialog.Builder(getActivity())
		    		.setTitle(getString(R.string.capture_dialog_title))
		    	    .setMessage(getString(R.string.capture_dialog_message))
		    	    .setView(input)
		    	    .setPositiveButton(getString(R.string.capture_dialog_accept), new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// Start logging
		    	        	String filename = input.getText().toString();
		    	        	
		    	        	boolean success;
				    		if (filename.isEmpty()) {success = Common.mService.startLogger(); }
				    		else { success = Common.mService.startLogger(filename); }
				    		
				    		if (success) {
				    			logButton.setText(R.string.stop_log);
						    	mLoggerStatus.setText(R.string.logging);
						    	
						    	// Fill additional information details
						    	if (filename.isEmpty()) { filename = Common.mService.getLoggerFileName(); }
						    	
						    	Time start = Common.mService.getLoggerStartTime();
						    	String startString = String.format("%02d:%02d:%02d",start.hour,start.minute,start.second);
						    	
						    	mFileName.setText(filename + ".log");
						    	mLogStartTime.setText(startString);
						    	mLogRuntime.setText("00:00:00");
						    	mLogSamples.setText("0");
						    	
						    	startRefreshingInfo();
				    		}
				    		else {
				    			Toast.makeText(getActivity(), R.string.error_fs_not_writable, Toast.LENGTH_LONG).show();
				    		}
		    	        }
		    	    }).setNegativeButton(getString(R.string.capture_dialog_cancel), null).show();
		    	}
		    }
		});
		
		return rootView;
	}
	
	@SuppressLint("DefaultLocale")
	@Override
	public void onResume() {
        super.onResume();
        registerReceiver();
        if (Common.isConnected) {
        	setUi(CONNECTED);
        }
        else if (Common.isConnecting) {
        	setUi(CONNECTING);
        }
        
        if (Common.mService.isLogging()) {
        	startRefreshingInfo();
        	mFileName.setText(Common.mService.getLoggerFileName() + ".log");
        	Time start = Common.mService.getLoggerStartTime();
	    	String startString = String.format("%02d:%02d:%02d",start.hour,start.minute,start.second);
        	mLogStartTime.setText(startString);
        	logButton.setText(R.string.stop_log);
        	mLoggerStatus.setText(R.string.logging);
        }
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stopRefreshingInfo();
		unRegisterReciever();
	}
	
	/*private void clearAdditionalInfo() {
		mFileName.setText("");
		mLogStartTime.setText("");
		mLogRuntime.setText("");
		mLogSamples.setText("");
	}*/
	
	private void setUi(int type) {
    	switch (type) {
    		case CONNECTED:
    			mConnectionState.setText(R.string.connected);
        		mDeviceName.setText(Common.deviceName);
        		mDeviceAddress.setText(Common.deviceAddress);
        		statusCard.setBackgroundResource(R.drawable.card_greenborder);
        		logButton.setText(R.string.start_log);
        		mLoggerStatus.setText(R.string.not_logging);
    			break;
    		case CONNECTING:
    			mConnectionState.setText(R.string.connecting);
    			mDeviceName.setText(Common.deviceName);
        		mDeviceAddress.setText(Common.deviceAddress);
        		statusCard.setBackgroundResource(R.drawable.card_goldborder);
        		logButton.setText(R.string.stop_log);
        		mLoggerStatus.setText(R.string.not_logging);
    			break;
    		case DISCONNECTED:
    			mConnectionState.setText(R.string.disconnected);
    			mDeviceName.setText("");
    			mDeviceAddress.setText("");
        		statusCard.setBackgroundResource(R.drawable.card_no_border);
        		logButton.setText(R.string.start_log);
        		mLoggerStatus.setText(R.string.not_logging);
    			break;
    	}
    }
	
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
    			  stopRefreshingInfo();
    			  setUi(DISCONNECTED);
    		  }
    	  }
    };
    
    private void stopRefreshingInfo() {
    	if (refresher != null) { refresher.interrupt(); }
    }
    
    private void startRefreshingInfo() {
    	refresher = new Thread() {
    		@SuppressLint("DefaultLocale")
			@Override
    		public void run() {
    			try {
    				while (true) {
	    				sleep(REFRESH_PERIOD);
	    				
	    				Time now = new Time();
	    				now.setToNow();
	    				long t1 = now.toMillis(false);
	    				long t2 = Common.mService.getLoggerStartTime().toMillis(false);
	    				long millis = t1-t2;
	    				
	    				final String runtime = String.format("%02d:%02d:%02d", 
	    						TimeUnit.MILLISECONDS.toHours(millis),
	    						TimeUnit.MILLISECONDS.toMinutes(millis) -  
	    						TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
	    						TimeUnit.MILLISECONDS.toSeconds(millis) - 
	    						TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
	    				final String samples = Integer.toString(Common.mService.getLoggerNumberOfSamples());
	    				
	    				getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mLogRuntime.setText(runtime);
								mLogSamples.setText(samples);
							}
						});
    				}
    			} catch (InterruptedException e) {
					return;
				}
    		}
    	};
    	refresher.start();
    }
}
