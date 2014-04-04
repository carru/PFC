package com.carruesco.pfc.rempark_rx;

import java.util.ArrayList;

import com.carruesco.pfc.rempark_rx.adapters.NavDrawerAdapter;
import com.carruesco.pfc.rempark_rx.adapters.NavDrawerItem;
import com.carruesco.pfc.rempark_rx.fragments.CaptureFragment;
import com.carruesco.pfc.rempark_rx.fragments.LiveFragment;
import com.carruesco.pfc.rempark_rx.fragments.ScanFragment;
import com.carruesco.pfc.rempark_rx.fragments.SettingsFragment;
import com.carruesco.pfc.rempark_rx.fragments.StoredFragment;

import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class MasterActivity extends Activity {
	// Main activity layout
    private DrawerLayout mDrawerLayout;
    // Navigation drawer list
    private ListView mDrawerList;
    
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    
    // List of items in the navigation drawer
    private ArrayList<NavDrawerItem> drawerItems;
    
    private void setupNavigationDrawer() {
    	mTitle = mDrawerTitle = getTitle();
        
    	// Get items
    	String[] titles = getResources().getStringArray(R.array.drawer_titles);
    	TypedArray icons = getResources().obtainTypedArray(R.array.drawer_icons);
    	// Add items to drawerItems
    	drawerItems = new ArrayList<NavDrawerItem>();
    	for (int i = 0; i < titles.length; i++) {
    		NavDrawerItem item = new NavDrawerItem(titles[i], icons.getResourceId(i, -1));
    		drawerItems.add(item);
		}
    	// Recycle TypedArray
    	icons.recycle();
    	
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new NavDrawerAdapter(this, drawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  // host Activity
                mDrawerLayout,         // DrawerLayout object
                R.drawable.ic_drawer,  // nav drawer image to replace 'Up' caret
                R.string.drawer_open,  // "open drawer" description for accessibility
                R.string.drawer_close  // "close drawer" description for accessibility
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.master_activity);
        
        // Set preferences to default values (only on first app launch, see third argument)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        // Initialize 'Common' class
        Common.init();
        Common.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Start service
        Intent intent = new Intent(this, BTService.class);
        startService(intent);
        
        // Setup navigation drawer
        setupNavigationDrawer();
        
        // Perform scan on startup?
        Common.scanOnStartup = Common.sharedPref.getBoolean("key_auto_scan", false);
        
        // Start with Device Scan fragment
        if (savedInstanceState == null) { selectItem(0); }
    }

    @Override
    protected void onDestroy() {
    	// Stop service
    	Intent intent = new Intent(this, BTService.class);
    	stopService(intent);
    	
    	super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        // Unbind from the service
        if (Common.mBound) {
            unbindService(mConnection);
            Common.mBound = false;
        }
        super.onPause();
    }
    
    @Override
    protected void onResume() {
    	// Bind to BleService
    	Intent intent = new Intent(this, BTService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    	super.onResume();
    }
    
    /** Defines callbacks for service binding, passed to bindService() */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	Common.mService = ((BTService.LocalBinder) service).getService();
            Common.mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	Common.mBound = false;
        }
    };
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	selectItem(position);
        }
    }

    private void selectItem(int position) {
    	Fragment fragment;
    	
    	// update the main content by replacing fragments
    	switch (position) {
			case 0: // Device Scan
				fragment = new ScanFragment(); break;
			case 1: // Live Data
				fragment = new LiveFragment(); break;
			case 2: // Capture
				fragment = new CaptureFragment(); break;
			case 3: // Stored Logs
				fragment = new StoredFragment(); break;
			case 4: // Settings
				fragment = new SettingsFragment(); break;
			default: // Device Scan (just in case)
				fragment = new ScanFragment(); break;
    	}
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(drawerItems.get(position).getTitle());
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

}