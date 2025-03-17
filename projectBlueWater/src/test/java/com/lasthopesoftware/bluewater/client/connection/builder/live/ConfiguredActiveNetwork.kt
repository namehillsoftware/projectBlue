package com.lasthopesoftware.bluewater.client.connection.builder.live

import com.lasthopesoftware.resources.network.LookupActiveNetwork
import java.net.NetworkInterface

class ConfiguredActiveNetwork(
	override val isNetworkActive: Boolean = false,
	override val isLocalNetworkActive: Boolean = false,
	override val activeNetworkInterface: NetworkInterface? = null
) : LookupActiveNetwork
