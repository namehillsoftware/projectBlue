package com.lasthopesoftware.resources.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.NetworkInterface

class ActiveNetworkFinder(context: Context) : CheckForActiveNetwork {
    private val connectivityManager by lazy {
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
	}

	override val isNetworkActive: Boolean
		get() =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				connectivityManager?.activeNetwork
					?.let { network -> connectivityManager?.getNetworkCapabilities(network) }
					?.let { capabilities ->
						when {
							capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
							capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
							capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
							capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
							else -> false
						}
					}
					?: false
			} else {
				@Suppress("DEPRECATION") // handled
				connectivityManager?.activeNetworkInfo?.isConnected ?: false
			}

	override val activeNetwork: NetworkInterface?
		get() {
			val activeNetworkName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				connectivityManager?.activeNetwork
					?.let { network -> connectivityManager?.getLinkProperties(network) }
					?.interfaceName
			} else {
				@Suppress("DEPRECATION") // handled
				connectivityManager?.activeNetworkInfo?.extraInfo
			}

			return NetworkInterface.getNetworkInterfaces().asSequence()
				.filter { i -> i.name == activeNetworkName }
				.firstOrNull()
		}
}
