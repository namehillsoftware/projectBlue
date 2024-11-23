package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnection
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnectionComplete
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnectionFailed
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.GettingLibrary
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.GettingLibraryFailed
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.SendingWakeSignal
import com.lasthopesoftware.resources.strings.GetStringResources

fun GetStringResources.getStatusString(status: BuildingConnectionStatus) = when (status) {
	GettingLibrary -> gettingLibrary
	GettingLibraryFailed -> gettingLibraryFailed
	SendingWakeSignal -> sendingWakeSignal
	BuildingConnection -> connectingToServerLibrary
	BuildingConnectionFailed -> errorConnectingTryAgain
	BuildingConnectionComplete -> connected
}
