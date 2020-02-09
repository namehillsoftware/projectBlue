package com.lasthopesoftware.bluewater.client.connection.builder.lookup

data class ServerInfo(
	val httpPort: Int = 0,
	val httpsPort: Int? = null,
	val remoteIp: String? = null,
	val localIps: Collection<String> = emptyList(),
	val macAddresses: Collection<String> = emptyList(),
	val certificateFingerprint: String? = null)
