package com.carruesco.pfc.sensortagrx.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;

public class Accelerometer extends Sensor {
	// UUID's
	private final static String SERV = "f000aa10-0451-4000-b000-000000000000";
	private final static String DATA = "f000aa11-0451-4000-b000-000000000000";
	private final static String CONF = "f000aa12-0451-4000-b000-000000000000";
	private final static String PERI = "f000aa13-0451-4000-b000-000000000000";
		
	// 0: disable, 1: enable
	private static final byte ENABLE_CODE = (byte)1;
	
	@Override
	public String getServUUID() { return SERV; }

	@Override
	public String getDataUUID() { return DATA; }

	@Override
	public String getConfUUID() { return CONF; }

	@Override
	public String getPeriUUID() { return PERI; }

	@Override
	public String getName() { return ACCELEROMETER; }
	
	@Override
	public byte[] getEnableCode() { return new byte[]{ENABLE_CODE}; }

	@Override
	public float[] parse(BluetoothGattCharacteristic characteristic) {
  		// The accelerometer has the range [-2g, 2g] with unit (1/64)g.
  		// To convert from unit (1/64)g to unit g we divide by 64.
  		// (g = 9.81 m/s^2)
		// X : Y : Z (3 bytes) 
		int x = characteristic.getIntValue(FORMAT_SINT8, 0);
        int y = characteristic.getIntValue(FORMAT_SINT8, 1);
        int z = characteristic.getIntValue(FORMAT_SINT8, 2);
        
        double scaledX = x / 64.0;
        double scaledY = y / 64.0;
        double scaledZ = z / 64.0;
        
        return new float[]{(float)scaledX, (float)scaledY, (float)scaledZ};
	}	
}
