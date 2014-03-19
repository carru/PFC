package com.example.sensortagrx;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

public abstract class Sensor {
	public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	public static final int MIN_PERIOD = 10; // 100 ms, 10 Hz
	
	// Sensor names
	protected static final String ACCELEROMETER = "Accelerometer";
	protected static final String MAGNETOMETER = "Magnetometer";
	protected static final String GYROSCOPE = "Gyroscope";
	
	// Get UUID's
	public abstract String getServUUID();
	public abstract String getDataUUID();
	public abstract String getConfUUID();
	public abstract String getPeriUUID();
	
	// Get enableCode
	public abstract byte[] getEnableCode();

	// Get sensor name
	public abstract String getName();
	
	// Parse sensor data
	public abstract float[] parse(BluetoothGattCharacteristic characteristic);
	
	public void setPeriod(BluetoothLeService mBluetoothLeService, int period) {
		// period in tens of millisecond
		// min: 100 ms (period = 10)
		// default: 1000 ms (period = 100)
		
		if (period<10) return;
		
		// Gyroscope's period can't be changed
		if (!getName().equals(ACCELEROMETER) && !getName().equals(MAGNETOMETER)) return;
		
		BluetoothGattService service = 
    			mBluetoothLeService.getService(UUID.fromString(getServUUID()));
  		BluetoothGattCharacteristic periCharacteristic = 
  				service.getCharacteristic(UUID.fromString(getPeriUUID()));
  		
  		Log.i(getName(), "Setting " + getName() + " period (writing characteristic) to " + period + "0 ms");
  		mBluetoothLeService.writeCharacteristic(periCharacteristic, new byte[]{(byte) period});
	}
	
	public void enable(BluetoothLeService mBluetoothLeService) {
		BluetoothGattService service = 
    			mBluetoothLeService.getService(UUID.fromString(getServUUID()));
  		BluetoothGattCharacteristic confCharacteristic = 
  				service.getCharacteristic(UUID.fromString(getConfUUID()));
  		
  		Log.i(getName(), "Enabling " + getName() + " sensor (writing characteristic)");
  		mBluetoothLeService.writeCharacteristic(confCharacteristic, getEnableCode());
	}
	
	public void enableNotifications(BluetoothLeService mBluetoothLeService) {
		BluetoothGattService service = 
    			mBluetoothLeService.getService(UUID.fromString(getServUUID()));
		BluetoothGattCharacteristic dataCharacteristic = 
				service.getCharacteristic(UUID.fromString(getDataUUID()));
		
  		Log.i(getName(), "Enabling " + getName() + " notifications");
  		mBluetoothLeService.setCharacteristicNotification(dataCharacteristic, true);
	}
	
	/**
     * Gyroscope, Magnetometer, Barometer, IR temperature
     * all store 16 bit two's complement values in the awkward format
     * LSB MSB, which cannot be directly parsed as getIntValue(FORMAT_SINT16, offset)
     * because the bytes are stored in the "wrong" direction.
     *
     * This function extracts these 16 bit two's complement values.
     * */
    protected static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(FORMAT_UINT8, offset);
        if (lowerByte == null) return 0;
        
        Integer upperByte = c.getIntValue(FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.
        if (upperByte == null) return 0;

        return (upperByte << 8) + lowerByte;
    }
}
