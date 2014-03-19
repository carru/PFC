package com.carruesco.pfc.sensortagrx.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.carruesco.pfc.sensortagrx.R;

//Custom adapter for the navigation drawer
public class NavDrawerAdapter extends BaseAdapter {
	private ArrayList<NavDrawerItem> navDrawerItems;
	private LayoutInflater mInflator;

	public NavDrawerAdapter(Activity activity, ArrayList<NavDrawerItem> navDrawerItems) {
		super();
		mInflator = activity.getLayoutInflater();
		this.navDrawerItems = navDrawerItems;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder;

		// General ListView optimization code.
		if (view == null) {
			view = mInflator.inflate(R.layout.drawer_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.title = (TextView) view.findViewById(R.id.title);
			viewHolder.icon = (ImageView) view.findViewById(R.id.icon);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		viewHolder.title.setText(navDrawerItems.get(i).getTitle());
		viewHolder.icon.setImageResource(navDrawerItems.get(i).getIcon());

		return view;
	}

	@Override
	public int getCount() { return navDrawerItems.size(); }

	@Override
	public Object getItem(int i) { return navDrawerItems.get(i); }

	@Override
	public long getItemId(int i) { return i; }

	class ViewHolder {
		TextView title;
		ImageView icon;
	}
}
