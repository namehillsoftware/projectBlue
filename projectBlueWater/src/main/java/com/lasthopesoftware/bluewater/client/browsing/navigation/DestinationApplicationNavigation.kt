package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate

class DestinationApplicationNavigation(
	private val inner: NavigateApplication,
	private val navController: NavController<Destination>,
	navigationMessages: RegisterForTypedMessages<NavigationMessage>,
	initialDestination: Destination?
) : NavigateApplication by inner, AutoCloseable {
	private val navigationMessageSubscription = navigationMessages.registerReceiver { message: NavigationMessage ->
		handleDirectDestination(message.destination)
	}

	init {
		if (initialDestination != null)
			handleDirectDestination(initialDestination)
	}

	override fun close() {
		navigationMessageSubscription.close()
	}

	private fun handleDirectDestination(destination: Destination) {
		when (destination) {
			is DownloadsScreen -> viewActiveDownloads(destination.libraryId)
			is ConnectionSettingsScreen -> viewServerSettings(destination.libraryId)
			is ApplicationSettingsScreen -> viewApplicationSettings()
			is LibraryScreen -> viewLibrary(destination.libraryId)
			is NowPlayingScreen -> viewNowPlaying(destination.libraryId)
			else -> navController.navigate(destination)
		}
	}
}
