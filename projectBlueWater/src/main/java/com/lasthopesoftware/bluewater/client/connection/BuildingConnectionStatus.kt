package com.lasthopesoftware.bluewater.client.connection

enum class BuildingConnectionStatus {
	GettingLibrary,
	SendingWakeSignal,
	GettingLibraryFailed,
	BuildingConnection,
	BuildingConnectionFailed,
	BuildingConnectionComplete
}
