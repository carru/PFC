package com.carruesco.pfc.sensortagrx;

import java.util.ArrayList;

import android.content.SharedPreferences;

public class FavouritesList {
	/* Keys to store the values
	favourites_list_size
	favourites_list_item_0
	favourites_list_item_1
	favourites_list_item_2
	[...]
	*/
	private static final String keyItems = "favourites_list_item";
	private static final String keyElements = "favourites_list_size";

	public static ArrayList<String> getList(SharedPreferences sharedPref) {
		int numberOfElements = sharedPref.getInt(keyElements, 0);
		ArrayList<String> list = new ArrayList<String>();
		
		for (int i = 0; i < numberOfElements; i++) {
			list.add(sharedPref.getString(keyItems + "_" + i, null));
		}
		
		return list;
	}
	
	public static boolean clear(SharedPreferences sharedPref) {
		SharedPreferences.Editor editor = sharedPref.edit();
		int numberOfElements = sharedPref.getInt(keyElements, 0);
		
		for (int i = 0; i < numberOfElements; i++) {
			editor.remove(keyItems + "_" + i);
		}
		editor.putInt(keyElements, 0);
		
		return editor.commit();
	}
	
	public static boolean save(SharedPreferences sharedPref, ArrayList<String> list) {
		SharedPreferences.Editor editor = sharedPref.edit();
		int numberOfElements = list.size();
		
		// First clear the original list
		if (clear(sharedPref)) {
			for (int i = 0; i < numberOfElements; i++) {
				editor.putString(keyItems + "_" + i, list.get(i));
			}
			editor.putInt(keyElements, numberOfElements);
			
			return editor.commit();
		}
		
		// Clear failed
		return false;
	}
}
