package com.carruesco.pfc.sensortagrx.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.carruesco.pfc.sensortagrx.R;

//Adapter for holding devices found through scanning.
public class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<Integer> mRssi;
    private LayoutInflater mInflator;

    public LeDeviceListAdapter(Activity activity) {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
        mRssi = new ArrayList<Integer>();
        mInflator = activity.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device, Integer rssi) {
    	int position = mLeDevices.indexOf(device);
    	
    	if (position == -1) {
    		// Not in the list
    		mLeDevices.add(device);
            mRssi.add(rssi);
    	} else {
    		// Already in the list
    		mLeDevices.set(position, device);
    		mRssi.set(position, rssi);
    	}
    }

    public BluetoothDevice getDevice(int position) { return mLeDevices.get(position); }

    public void clear() {
        mLeDevices.clear();
        mRssi.clear();
    }

    @Override
    public int getCount() { return mLeDevices.size(); }

    @Override
    public Object getItem(int i) { return mLeDevices.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText(R.string.unknown_device);
        }
        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.deviceRssi.setText(mRssi.get(i).toString());

        return view;
    }

	static class ViewHolder {
	    TextView deviceName;
	    TextView deviceAddress;
	    TextView deviceRssi;
	}
}
