package com.lasthopesoftware.resources.network

import java.net.NetworkInterface

interface LookupActiveNetwork {
	val isNetworkActive: Boolean
	val isLocalNetworkActive: Boolean
	val activeNetworkInterface: NetworkInterface?
}
