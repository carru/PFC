package com.carruesco.pfc.rempark_rx.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

public class Magnetometer extends Sensor {
	// UUID's
	private final static String SERV = "f000aa30-0451-4000-b000-000000000000";
	private final static String DATA = "f000aa31-0451-4000-b000-000000000000";
	private final static String CONF = "f000aa32-0451-4000-b000-000000000000";
	private final static String PERI = "f000aa33-0451-4000-b000-000000000000";
		
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
	public String getName() { return MAGNETOMETER; }

	@Override
	public byte[] getEnableCode() { return new byte[]{ENABLE_CODE}; }
	
	@Override
	public float[] parse(BluetoothGattCharacteristic characteristic) {
		// XLSB XMSB YLSB YMSB ZLSB ZMSB 
		float x = Sensor.shortSignedAtOffset(characteristic, 0) * (2000f / 65536f);
        float y = Sensor.shortSignedAtOffset(characteristic, 2) * (2000f / 65536f);
        float z = Sensor.shortSignedAtOffset(characteristic, 4) * (2000f / 65536f);
		
        return new float[]{x, y, z};
	}
}
