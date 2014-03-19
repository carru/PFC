package com.carruesco.pfc.sensortagrx.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

public class Gyroscope extends Sensor {
	// UUID's
	private final static String SERV = "f000aa50-0451-4000-b000-000000000000";
	private final static String DATA = "f000aa51-0451-4000-b000-000000000000";
	private final static String CONF = "f000aa52-0451-4000-b000-000000000000";
		
	// 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
	private static final byte ENABLE_CODE = (byte)7;
	
	@Override
	public String getServUUID() { return SERV; }

	@Override
	public String getDataUUID() { return DATA; }

	@Override
	public String getConfUUID() { return CONF; }

	@Override
	public String getPeriUUID() { return ""; }

	@Override
	public String getName() { return GYROSCOPE; }

	@Override
	public byte[] getEnableCode() { return new byte[]{ENABLE_CODE}; }
	
	@Override
	public float[] parse(BluetoothGattCharacteristic characteristic) {
		// XLSB XMSB YLSB YMSB ZLSB ZMSB 
		float x = Sensor.shortSignedAtOffset(characteristic, 0) * (500f / 65536f);
        float y = Sensor.shortSignedAtOffset(characteristic, 2) * (500f / 65536f);
        float z = Sensor.shortSignedAtOffset(characteristic, 4) * (500f / 65536f);

        return new float[]{x, y, z};
	}
}
