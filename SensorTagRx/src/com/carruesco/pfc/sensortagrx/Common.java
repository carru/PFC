package com.carruesco.pfc.sensortagrx;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;

// Class used to store common members accessed by app fragments
// and that have to be maintained
public class Common {
	public static BluetoothAdapter mBluetoothAdapter;
	public static String deviceName;
	public static String deviceAddress;
	public static boolean isConnected;
	public static boolean isConnecting;
	
	public static boolean scanOnStartup;
	
	// Preferences
	public static SharedPreferences sharedPref;
	
	// BLE Service
	public static BleService mService;
	public static boolean mBound;
	
	public static void init() {
		mBluetoothAdapter = null;
		deviceName = null;
		deviceAddress = null;
		isConnected = false;
		
		mService = null;
		mBound = false;
		
		sharedPref = null;
		
		scanOnStartup = false;
	}
	
	public static void disconnect() {
		if (mService != null) {	mService.disconnect(); }
		isConnected = false;
		isConnecting = false;
		deviceName = null;
		deviceAddress = null;
	}
	
	public static boolean connect() {
		if (deviceAddress.isEmpty()) { return false; }
		
		isConnected = false;
		isConnecting = mService.connect();
		return isConnecting;
	}
}
