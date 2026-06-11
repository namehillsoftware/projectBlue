package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideProgressingLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibrarySelectionNavigation
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate

class RoutedNavigationDependencies(
	inner: EntryDependencies,
	innerNavigation: NavigateApplication,
	connectionStatusViewModel: ConnectionStatusViewModel,
	private val navController: NavController<Destination>
) : EntryDependencies by inner, AutoCloseable {
	private val navigationMessageSubscription = navigationMessages.registerReceiver { message: NavigationMessage ->
		promiseNavigation(message.destination)
	}

	override val applicationNavigation by lazy {
		LibrarySelectionNavigation(
			innerNavigation,
			selectedLibraryViewModel,
			connectionStatusViewModel,
		)
	}

	override val libraryConnectionProvider: ProvideLibraryConnections = connectionStatusViewModel

	override val progressingLibraryConnectionProvider: ProvideProgressingLibraryConnections = connectionStatusViewModel

	override fun close() {
		navigationMessageSubscription.close()
	}

	fun promiseNavigation(destination: Destination): Promise<Unit> = with (applicationNavigation) {
		when (destination) {
			is DownloadsScreen -> viewActiveDownloads(destination.libraryId)
			is ConnectionSettingsScreen -> viewServerSettings(destination.libraryId)
			is ApplicationSettingsScreen -> viewApplicationSettings()
			is LibraryScreen -> viewLibrary(destination.libraryId)
			is NowPlayingScreen -> viewNowPlaying(destination.libraryId)
			is ActiveLibraryDownloadsScreen -> viewActiveDownloads()
			is FilePropertySearchScreen -> {
				val filePropertyFilter = destination.filePropertyFilter

				if (filePropertyFilter != null)
					search(destination.libraryId, filePropertyFilter)
				else
					launchSearch(destination.libraryId)
			}
			is SearchScreen -> {
				search(destination.libraryId, destination.searchQuery)
			}

			else -> navController.navigate(destination).toPromise()
		}
	}
}
