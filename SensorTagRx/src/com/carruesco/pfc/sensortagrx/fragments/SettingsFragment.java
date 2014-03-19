package com.carruesco.pfc.sensortagrx.fragments;

import com.carruesco.pfc.sensortagrx.R;

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
                            //TODO
                        	Toast.makeText(getActivity(), R.string.favourites_cleared, Toast.LENGTH_SHORT).show();
                            return true;
                        }
        });
    }
}
