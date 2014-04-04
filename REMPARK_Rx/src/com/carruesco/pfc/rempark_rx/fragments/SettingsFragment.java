package com.carruesco.pfc.rempark_rx.fragments;

import com.carruesco.pfc.rempark_rx.Common;
import com.carruesco.pfc.rempark_rx.FavouritesList;
import com.carruesco.pfc.rempark_rx.R;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        Preference clearFavourites = (Preference)findPreference("key_clear_favourites");
        clearFavourites.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) { 
                        	if (FavouritesList.clear(Common.sharedPref)) {
                        		Toast.makeText(getActivity(), R.string.favourites_cleared, Toast.LENGTH_SHORT).show();
                        	}
                        	else {
                        		Toast.makeText(getActivity(), R.string.favourites_cleared_error, Toast.LENGTH_SHORT).show();
                        	}
                        	
                            return true;
                        }
        });
    }
}
