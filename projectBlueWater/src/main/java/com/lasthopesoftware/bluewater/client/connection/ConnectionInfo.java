package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by david on 8/8/15.
 */
public class ConnectionInfo {

	// Utility methods. Questionable location for these methods
	public static int getConnectionType(Context context) {
		final NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
		return activeNetworkInfo == null ? -1 : activeNetworkInfo.getType();
	}

	public static NetworkInfo getActiveNetworkInfo(Context context) {
		final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connectivityManager.getActiveNetworkInfo();
	}
}
