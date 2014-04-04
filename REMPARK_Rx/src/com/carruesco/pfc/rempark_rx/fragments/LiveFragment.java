package com.carruesco.pfc.rempark_rx.fragments;

import com.carruesco.pfc.rempark_rx.BTService;
import com.carruesco.pfc.rempark_rx.Common;
import com.carruesco.pfc.rempark_rx.R;
import com.carruesco.pfc.rempark_rx.SensorSample;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class LiveFragment extends Fragment{
	// UI references
	private TextView Ax,Ay,Az,Mx,My,Mz,Gx,Gy,Gz;
	private Button calibrateButton;
	
	private void getUiReferences(View v) {
		Ax = (TextView) v.findViewById(R.id.live_accelerometer_x);
		Ay = (TextView) v.findViewById(R.id.live_accelerometer_y);
		Az = (TextView) v.findViewById(R.id.live_accelerometer_z);
		Mx = (TextView) v.findViewById(R.id.live_magnetometer_x);
		My = (TextView) v.findViewById(R.id.live_magnetometer_y);
		Mz = (TextView) v.findViewById(R.id.live_magnetometer_z);
		Gx = (TextView) v.findViewById(R.id.live_gyroscope_x);
		Gy = (TextView) v.findViewById(R.id.live_gyroscope_y);
		Gz = (TextView) v.findViewById(R.id.live_gyroscope_z);
		
		calibrateButton = (Button) v.findViewById(R.id.calibrate_button);
	}
	
	// Empty constructor required for fragment subclasses
	public LiveFragment() {}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.live_fragment, container, false);
		
		getUiReferences(rootView);
		
		calibrateButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	if (Common.isConnected) {
		    		Common.mService.calibrateMagnetometer();
		    	}
		    }
		});
		
		return rootView;
	}
	
	@Override
	public void onPause() {
		registerReceiver();
		BTService.broadcastData = false;
		super.onPause();
	}
	
	@Override
	public void onResume() {
		registerReciever();
		BTService.broadcastData = true;
		super.onResume();
	}
	
	private void displayData(SensorSample sample) {
		if (sample.getName().equals(Common.mService.getAccelerometerName())) {
			Ax.setText(Float.toString(sample.getX()));
			Ay.setText(Float.toString(sample.getY()));
			Az.setText(Float.toString(sample.getZ()));
		}
		else if (sample.getName().equals(Common.mService.getMagnetometerName())) {
			Mx.setText(Float.toString(sample.getX()));
			My.setText(Float.toString(sample.getY()));
			Mz.setText(Float.toString(sample.getZ()));
		}
		else if (sample.getName().equals(Common.mService.getGyroscopeName())) {
			Gx.setText(Float.toString(sample.getX()));
			Gy.setText(Float.toString(sample.getY()));
			Gz.setText(Float.toString(sample.getZ()));
		}
	}
	
	private SensorSample getData(Intent intent) {
    	float[] data = intent.getFloatArrayExtra(BTService.VALUE);
        String sensorName = intent.getStringExtra(BTService.SENSOR_TYPE);
        return new SensorSample(data, sensorName);
    }
	
	private void registerReciever() {
    	LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
    			makeGattUpdateIntentFilter());
    }
    
    private void registerReceiver() {
    	LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }
    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    		  displayData(getData(intent));
    	  }
    };
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BTService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
