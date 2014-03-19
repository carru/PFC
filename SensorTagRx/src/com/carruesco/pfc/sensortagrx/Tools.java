package com.carruesco.pfc.sensortagrx;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Tools {
	public static void showToast(Context context, String string) {
		Toast.makeText(context, string, Toast.LENGTH_LONG).show();
	}
	public static void log(String text) {
		Log.i("TESTS", text + " - Thread id: " + Long.toString(Thread.currentThread().getId()));
	}
}
