package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializingLibrarySelectionNavigation
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import dev.olshevski.navigation.reimagined.NavController

class RoutedNavigationDependencies(
    inner: BrowserViewDependencies,
    graphNavigation: NavigateApplication,
    override val connectionStatusViewModel: ConnectionStatusViewModel,
    navController: NavController<Destination>,
    initialDestination: Destination?
) : BrowserViewDependencies by inner, AutoCloseable {
	private val closeableManager = AutoCloseableManager()

	override val applicationNavigation by lazy {
		closeableManager.manage(
            DestinationApplicationNavigation(
                ConnectionInitializingLibrarySelectionNavigation(
                    graphNavigation,
                    selectedLibraryViewModel,
                    connectionStatusViewModel,
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
