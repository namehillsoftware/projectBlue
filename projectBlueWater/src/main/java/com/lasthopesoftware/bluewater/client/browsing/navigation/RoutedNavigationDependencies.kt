package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibrarySelectionNavigation
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import dev.olshevski.navigation.reimagined.NavController

class RoutedNavigationDependencies(
	inner: EntryDependencies,
	graphNavigation: NavigateApplication,
	override val libraryConnectionProvider: ProvideLibraryConnections,
	navController: NavController<Destination>,
	initialDestination: Destination?
) : EntryDependencies by inner, AutoCloseable {
	private val closeableManager = AutoCloseableManager()

	override val applicationNavigation by lazy {
		closeableManager.manage(
            DestinationApplicationNavigation(
                LibrarySelectionNavigation(
					graphNavigation,
					selectedLibraryViewModel,
                ),
                navController,
                navigationMessages,
                initialDestination
            )
		)
	}

	override fun close() {
		closeableManager.close()
	}
}
