package com.lasthopesoftware.bluewater.shared;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionInfo;

/**
 * Created by david on 7/24/15.
 */
public class IoCommon {
	public static final String FileUriScheme = "file";

	public static boolean isWifiAndPowerConnected(Context context) {
		if (ConnectionInfo.getConnectionType(context) != ConnectivityManager.TYPE_WIFI) return false;

		final Intent batteryStatusReceiver = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if (batteryStatusReceiver == null) return false;

		final int pluggedStatus = batteryStatusReceiver.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return pluggedStatus > 0;
	}
}
