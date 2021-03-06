package com.carruesco.pfc.rempark_rx.fragments;

import java.text.DecimalFormat;

import com.carruesco.pfc.rempark_rx.BTService;
import com.carruesco.pfc.rempark_rx.BTWorker;
import com.carruesco.pfc.rempark_rx.R;
import com.carruesco.pfc.rempark_rx.sensor.MultiSample;
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
import android.widget.TextView;

public class LiveFragment extends Fragment{
	// UI references
	private TextView Ax,Ay,Az,Mx,My,Mz,Gx,Gy,Gz;
	//private Button calibrateButton;
	
	// To format sample values
	DecimalFormat decimalFormat = new DecimalFormat("0.0000");
	
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
		
		//calibrateButton = (Button) v.findViewById(R.id.calibrate_button);
	}
	
	// Empty constructor required for fragment subclasses
	public LiveFragment() {}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.live_fragment, container, false);
		
		getUiReferences(rootView);
		
		/*calibrateButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	if (Common.isConnected) {
		    		BTWorker.magnetometerIsCalibrating = true;
		    	}
		    }
		});*/
		
		return rootView;
	}
	
	@Override
	public void onPause() {
		unregisterReceiver();
		BTWorker.broadcastSamples = false;
		super.onPause();
	}
	
	@Override
	public void onResume() {
		registerReceiver();
		BTWorker.broadcastSamples = true;
		super.onResume();
	}
	
	private void displayData(MultiSample sample) {
		decimalFormat.applyPattern("0.0000");
		Ax.setText(decimalFormat.format(sample.accelerometer.getX()));
		Ay.setText(decimalFormat.format(sample.accelerometer.getY()));
		Az.setText(decimalFormat.format(sample.accelerometer.getZ()));
		
		Gx.setText(decimalFormat.format(sample.gyroscope.getX()));
		Gy.setText(decimalFormat.format(sample.gyroscope.getY()));
		Gz.setText(decimalFormat.format(sample.gyroscope.getZ()));
		
		decimalFormat.applyPattern("0.000000"); // More decimals for the magnetometer
		Mx.setText(decimalFormat.format(sample.magnetometer.getX()));
		My.setText(decimalFormat.format(sample.magnetometer.getY()));
		Mz.setText(decimalFormat.format(sample.magnetometer.getZ()));
	}
	
	private MultiSample getData(Intent intent) {
    	double[] data = intent.getDoubleArrayExtra(BTService.VALUE);
        return new MultiSample(data);
    }
	
	private void registerReceiver() {
    	LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
    			makeGattUpdateIntentFilter());
    }
    
    private void unregisterReceiver() {
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
