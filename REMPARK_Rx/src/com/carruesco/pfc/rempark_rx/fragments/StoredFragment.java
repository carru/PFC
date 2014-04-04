package com.carruesco.pfc.rempark_rx.fragments;

import java.io.File;

import com.carruesco.pfc.rempark_rx.R;
import com.carruesco.pfc.rempark_rx.adapters.LogsListAdapter;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class StoredFragment extends Fragment {
	// Delete button
	private Button deleteAllButton;
	
	// List of logs
	private ListView logsList;
	private LogsListAdapter adapter;
	
	// Empty constructor required for fragment subclasses
	public StoredFragment() {}
		
	private void getUiReferences(View v) {
		deleteAllButton = (Button) v.findViewById(R.id.delete_all_button);
		logsList = (ListView) v.findViewById(R.id.logs_list);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.stored_fragment, container, false);
		
		getUiReferences(rootView);
		
		deleteAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Ask for confirmation
	    		new AlertDialog.Builder(getActivity())
	    	    .setMessage(getString(R.string.stored_delete_all_logs_dialog))
	    	    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	deleteAllLogs();
	    				adapter.refresh();
	    				Toast.makeText(getActivity(), R.string.stored_all_logs_deleted,
	    						Toast.LENGTH_SHORT).show();
	    	        }
	    	    }).setNegativeButton(getString(R.string.no), null).show();
			}
		});
		
		// Set ListView adapter
		adapter = new LogsListAdapter(getActivity(),shareListener,deleteListener);
		logsList.setAdapter(adapter);
		
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Update the list
		adapter.refresh();
	}
		
	// Delete all the logs in the folder
	private void deleteAllLogs() {
		File root = new File(getActivity().getExternalFilesDir(null), "");

		// All files in our directory
		File[] files = root.listFiles();

		// Loop through all files and delete them
		for (File file : files) {
			if (file.isFile()) { file.delete(); }
		}
	}
	
	// Share button listener
	private OnClickListener shareListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int position = (Integer) v.getTag();
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			File file = new File(getActivity().getExternalFilesDir(null), adapter.getItem(position));
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
			shareIntent.setType("text/rtf");
			String s = getString(R.string.stored_share_intent_title) + " " + adapter.getItem(position) + ":";
			startActivity(Intent.createChooser(shareIntent, s));
		}
	};
	
	// Delete button listener
	private OnClickListener deleteListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final int position = (Integer) v.getTag();
			String s = getString(R.string.stored_delete_log_dialog) + " " +
					adapter.getItem(position) + "?";
			// Ask for confirmation
    		new AlertDialog.Builder(getActivity())
    	    .setMessage(s)
    	    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
    	        public void onClick(DialogInterface dialog, int whichButton) {
    	        	deleteLog(adapter.getItem(position));
    				adapter.refresh();
    	        }
    	    }).setNegativeButton(getString(R.string.no), null).show();
		}
	};
	
	private void deleteLog(String name){
		File file = new File(getActivity().getExternalFilesDir(null), name);
		file.delete();
	}
}
