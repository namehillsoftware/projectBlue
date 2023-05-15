package com.lasthopesoftware.resources.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class ActiveNetworkFinder(context: Context) : LookupActiveNetwork {
    private val lazyConnectivityManager by lazy {
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	}

    override val activeNetworkInfo: NetworkInfo?
        get() = lazyConnectivityManager.activeNetworkInfo
}
