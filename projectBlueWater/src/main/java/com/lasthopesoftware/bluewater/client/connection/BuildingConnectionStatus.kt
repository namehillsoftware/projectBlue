package com.lasthopesoftware.bluewater.client.connection

enum class BuildingConnectionStatus {
	GettingLibrary,
	GettingLibraryFailed,
	SendingWakeSignal,
	SendingWakeSignalFailed,
	BuildingConnection,
	BuildingConnectionFailed,
	BuildingConnectionComplete
}
