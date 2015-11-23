package org.hpsaturn.autowifi.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.hpsaturn.autowifi.Config;


public class StartStatusServiceReceiver extends BroadcastReceiver {
	
	public static final String TAG = StartStatusServiceReceiver.class.getSimpleName();
	private static final boolean DEBUG = Config.DEBUG;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(DEBUG) Log.d(TAG, "StartServiceReceiver: onReceive");
		Intent service = new Intent(context, StatusService.class);
		context.startService(service);
		
	}
}
