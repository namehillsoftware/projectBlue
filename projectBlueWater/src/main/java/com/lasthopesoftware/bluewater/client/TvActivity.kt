package com.lasthopesoftware.bluewater.client

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.TvItemView
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.AboutScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.TvApplicationSettingsView
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController

class TvActivity :
	AppCompatActivity(),
	PermissionsDependencies,
	ManagePermissions
{
	private val dependencies by lazy { ActivityDependencies(this) }

	override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onCreate(savedInstanceState, persistentState)

		setContent {
			ProjectBlueTheme {
				CatalogBrowser(dependencies, this)
			}
		}
	}

	override val readPermissionsRequirements by lazy { ApplicationReadPermissionsRequirementsProvider(this) }
	override val writePermissionsRequirements by lazy { ApplicationWritePermissionsRequirementsProvider(this) }
	override val permissionsManager = this
	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> = permissions.associateWith { false }.toPromise()
}

private class TvNavigation(
	private val navController: NavController<Destination>
) : NavigateApplication {
	override fun launchSearch(libraryId: LibraryId): Promise<Unit> {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(SearchScreen(libraryId))

		return Unit.toPromise()
	}

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))

		return Unit.toPromise()
	}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> {
		if (item is Item)
			navController.navigate(ItemScreen(libraryId, item))

		return Unit.toPromise()
	}

//	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
//		return inner.viewFileDetails(libraryId, playlist, position)
//	}

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> {
		if (!navController.moveToTop { it is NowPlayingScreen }) {
			navController.navigate(NowPlayingScreen(libraryId))
		}

		return Unit.toPromise()
	}
}

@Composable
fun TvSettingsView() {

}

private class TvDependencies(
	private val inner: BrowserViewDependencies,
 	override val applicationNavigation: NavigateApplication
) : BrowserViewDependencies by inner

@Composable
fun CatalogBrowser(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
) {
	val navController = rememberNavController(
		listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
	)

	val graphNavigation = remember {
		TvNavigation(navController)
	}

	val tvDependencies = TvDependencies(browserViewDependencies, graphNavigation)

	NavHost(navController) { destination ->
		LocalViewModelStoreOwner.current
			?.let {
				ScopedViewModelDependencies(tvDependencies, permissionsDependencies, it)
			}
			?.apply {
				when (destination) {
					AboutScreen -> {}
					ApplicationSettingsScreen -> {
						TvApplicationSettingsView(
							applicationSettingsViewModel,
							applicationNavigation,
							playbackServiceController,
						)
					}
					ActiveLibraryDownloadsScreen -> {}
					SelectedLibraryReRouter -> {}
					HiddenSettingsScreen -> {}
					is DownloadsScreen -> {}
					is ItemScreen -> {
						TvItemView(
							itemListViewModel = itemListViewModel,
							fileListViewModel = fileListViewModel,
							navigateApplication = applicationNavigation,
							trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
						)
					}

					is LibraryScreen -> {}
					is SearchScreen -> {}
					is ConnectionSettingsScreen -> {}
					is NowPlayingScreen -> {}
					NewConnectionSettingsScreen -> {}
				}
			}
	}
}
