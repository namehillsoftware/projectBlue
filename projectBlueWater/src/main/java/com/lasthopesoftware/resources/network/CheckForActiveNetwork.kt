package com.lasthopesoftware.resources.network

import java.net.NetworkInterface

interface CheckForActiveNetwork {
	val isNetworkActive: Boolean
	val activeNetwork: NetworkInterface?
}
