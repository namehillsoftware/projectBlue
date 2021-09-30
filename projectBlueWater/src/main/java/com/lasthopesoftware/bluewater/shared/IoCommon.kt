package com.lasthopesoftware.bluewater.shared

import android.content.Context
import android.net.ConnectivityManager
import com.lasthopesoftware.bluewater.client.connection.ConnectionInfo

object IoCommon {
	const val FileUriScheme = "file"
	const val httpUriScheme = "http"
	const val httpsUriScheme = "https"

	fun isWifiConnected(context: Context?): Boolean =
		ConnectionInfo.getConnectionType(context) == ConnectivityManager.TYPE_WIFI
}
