package com.lasthopesoftware.bluewater.client.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * Created by david on 8/8/15.
 */
object ConnectionInfo {
    // Utility methods. Questionable location for these methods
    fun getConnectionType(context: Context): Int {
        val activeNetworkInfo = getActiveNetworkInfo(context)
        return activeNetworkInfo?.type ?: -1
    }

    fun getActiveNetworkInfo(context: Context): NetworkInfo? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager?.activeNetworkInfo
    }
}
