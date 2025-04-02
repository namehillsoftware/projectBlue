package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideProgressingLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibrarySelectionNavigation
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import dev.olshevski.navigation.reimagined.NavController

class RoutedNavigationDependencies(
	inner: EntryDependencies,
	innerNavigation: NavigateApplication,
	connectionStatusViewModel: ConnectionStatusViewModel,
	navController: NavController<Destination>,
	initialDestination: Destination?
) : EntryDependencies by inner, AutoCloseable {
	private val closeableManager = AutoCloseableManager()

	override val applicationNavigation by lazy {
		closeableManager.manage(
            DestinationApplicationNavigation(
                LibrarySelectionNavigation(
					innerNavigation,
					selectedLibraryViewModel,
					connectionStatusViewModel,
                ),
                navController,
                navigationMessages,
                initialDestination
            )
		)
	}

	override val libraryConnectionProvider: ProvideLibraryConnections = connectionStatusViewModel
	override val progressingLibraryConnectionProvider: ProvideProgressingLibraryConnections = connectionStatusViewModel

	override fun close() {
		closeableManager.close()
	}
}
