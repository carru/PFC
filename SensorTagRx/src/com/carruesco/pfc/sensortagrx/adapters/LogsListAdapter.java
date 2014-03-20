package com.carruesco.pfc.sensortagrx.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.carruesco.pfc.sensortagrx.Common;
import com.carruesco.pfc.sensortagrx.R;

//Custom adapter for the list
public class LogsListAdapter extends BaseAdapter {
	private ArrayList<String> list;
	private LayoutInflater mInflator;
	private Activity activity;
	
	private OnClickListener shareListener;
	private OnClickListener deleteListener;

	public LogsListAdapter(Activity activity, OnClickListener shareListener, OnClickListener deleteListener) {
		super();

		list = new ArrayList<String>();
		mInflator = activity.getLayoutInflater();
		this.activity = activity;
		this.shareListener = shareListener;
		this.deleteListener = deleteListener;
	}

	public void refresh() {
		File root = new File(activity.getExternalFilesDir(null), "");
		File[] files = root.listFiles();

		list.clear();

		// Are we logging right now?
		if (Common.mService.isLogging()) {
			// Yes, do not include the current log in the list
			String currentLog = Common.mService.getLoggerFileName();
			for (File file : files) {
				if (file.isFile()) {
					if (!file.getName().equals(currentLog + ".log")) {
						list.add(file.getName());
					}
				}
			}
		} else {
			// No
			for (File file : files) {
				if (file.isFile()) {
					list.add(file.getName());
				}
			}
		}

		// Sort alphabetically
		Collections.sort(list);
		
		notifyDataSetChanged();
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder;

		// General ListView optimization code.
		if (view == null) {
			view = mInflator.inflate(R.layout.log_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.logFileName = (TextView) view
					.findViewById(R.id.logs_list_file_name);
			viewHolder.logShareButton = (ImageView) view
					.findViewById(R.id.logs_list_share_button);
			viewHolder.logDeleteButton = (ImageView) view
					.findViewById(R.id.logs_list_delete_button);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		viewHolder.logFileName.setText(list.get(i));

		viewHolder.logShareButton.setTag((Integer) i);
		viewHolder.logShareButton.setOnClickListener(shareListener);

		viewHolder.logDeleteButton.setTag((Integer) i);
		viewHolder.logDeleteButton.setOnClickListener(deleteListener);

		return view;
	}

	@Override
	public int getCount() { return list.size(); }

	@Override
	public String getItem(int i) { return list.get(i); }

	@Override
	public long getItemId(int i) { return i; }

	static class ViewHolder {
		TextView logFileName;
		ImageView logShareButton;
		ImageView logDeleteButton;
	}
}
