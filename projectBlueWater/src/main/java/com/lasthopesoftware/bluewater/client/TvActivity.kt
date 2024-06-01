package com.lasthopesoftware.bluewater.client

import LoadedItemListView
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.TvInterfaceDependencies
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.image.LibraryItemImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableTvChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializingLibrarySelectionNavigation
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.settings.TvLibrarySettingsView
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.TvApplicationSettingsView
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import java.util.concurrent.ConcurrentHashMap

private val logger by lazyLogger<TvActivity>()

@UnstableApi
class TvActivity :
	AppCompatActivity(),
	PermissionsDependencies,
	ManagePermissions,
	ActivitySuppliedDependencies {
	private val dependencies by lazy { ActivityDependencies(this, this) }

	override val registeredActivityResultsLauncher = registerResultActivityLauncher()

	override val applicationPermissions by lazy {
		val osPermissionChecker = OsPermissionsChecker(applicationContext)
		ApplicationPermissionsRequests(
			dependencies.libraryProvider,
			ApplicationReadPermissionsRequirementsProvider(osPermissionChecker),
			this,
			osPermissionChecker
		)
	}

	private val permissionsRequests = ConcurrentHashMap<Int, Messenger<Map<String, Boolean>>>()

	override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onCreate(savedInstanceState, persistentState)

		setContent {
			ProjectBlueTheme {
				CatalogBrowser(dependencies, this)
			}
		}
	}

	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> {
		return if (permissions.isEmpty()) Promise(emptyMap())
		else Promise<Map<String, Boolean>> { messenger ->
			val requestId = messenger.hashCode()
			permissionsRequests[requestId] = messenger

			ActivityCompat.requestPermissions(
				this,
				permissions.toTypedArray(),
				requestId
			)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		permissionsRequests
			.remove(requestCode)
			?.sendResolution(
				grantResults
					.zip(permissions)
					.associate { (r, p) -> Pair(p, r == PackageManager.PERMISSION_GRANTED) }
			)
	}
}

private class TvNavigation(
	private val inner: NavigateApplication,
	private val navController: NavController<Destination>
) : NavigateApplication by inner {
	override fun viewApplicationSettings(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }

		return Unit.toPromise()
	}

	override fun viewNewServerSettings(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(NewConnectionSettingsScreen)

		return Unit.toPromise()
	}

	override fun viewServerSettings(libraryId: LibraryId): Promise<Unit> {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(ConnectionSettingsScreen(libraryId))

		return Unit.toPromise()
	}

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
		return Unit.toPromise()
	}

	override fun backOut(): Promise<Boolean> = navigateUp()

	override fun navigateUp(): Promise<Boolean> {
		return if (navController.pop() && navController.backstack.entries.any()) true.toPromise()
		else inner.navigateUp()
	}
}

private class TvDependencies(
	private val inner: BrowserViewDependencies,
	private val tvNavigation: TvNavigation,
	override val connectionStatusViewModel: ConnectionStatusViewModel,
) : BrowserViewDependencies by inner, TvInterfaceDependencies {

	override val reusableTvChildItemViewModelProvider by lazy {
		ReusableTvChildItemViewModelProvider(
			LibraryItemImageProvider(RemoteImageAccess(libraryConnectionProvider))
		)
	}

	override val applicationNavigation by lazy {
		ConnectionInitializingLibrarySelectionNavigation(
			tvNavigation,
			selectedLibraryViewModel,
			connectionStatusViewModel,
		)
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserMediaView(viewModelDependencies: ScopedViewModelDependencies, connectionStatusViewModel: ConnectionStatusViewModel, libraryId: LibraryId, item: Item?) {
	with (viewModelDependencies) {
		Row(
			modifier = Modifier.fillMaxSize()
		) {
			Box(modifier = Modifier
				.weight(1f)
				.fillMaxHeight()
			) {
				LoadedItemListView(
					viewModelDependencies,
					libraryId,
					item
				)
			}

			Box(modifier = Modifier
				.weight(2f)
				.fillMaxHeight()
			) {
				val screenViewModel = viewModel {
					NowPlayingScreenViewModel(
						messageBus,
						InMemoryNowPlayingDisplaySettings,
						playbackServiceController,
					)
				}

				NowPlayingView(
					nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					screenOnState = screenViewModel,
					playbackServiceController = playbackServiceController,
					playlistViewModel = nowPlayingPlaylistViewModel,
					childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					connectionWatcherViewModel = connectionWatcherViewModel,
					viewModelMessageBus = nowPlayingViewModelMessageBus
				)
			}
		}
	}
}

@Composable
fun CatalogBrowser(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
) {
	val navController = rememberNavController(
		listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
	)

	val graphNavigation = remember {
		TvNavigation(browserViewDependencies.applicationNavigation, navController)
	}

	val connectionStatusViewModel = viewModel {
		ConnectionStatusViewModel(
			browserViewDependencies.stringResources,
			DramaticConnectionInitializationController(
				browserViewDependencies.libraryConnectionProvider,
				graphNavigation,
			),
		)
	}

	val tvDependencies = remember { TvDependencies(browserViewDependencies, graphNavigation, connectionStatusViewModel) }

	BackHandler { tvDependencies.applicationNavigation.backOut() }

	ControlSurface {
		NavHost(navController) { destination ->
			LocalViewModelStoreOwner.current
				?.let {
					ScopedViewModelDependencies(tvDependencies, permissionsDependencies, it)
				}
				?.apply {
					when (destination) {
						ApplicationSettingsScreen -> {
							TvApplicationSettingsView(
								applicationSettingsViewModel,
								applicationNavigation,
								playbackServiceController,
							)

							applicationSettingsViewModel.loadSettings()
						}

						ActiveLibraryDownloadsScreen -> {}
						SelectedLibraryReRouter -> {
							LaunchedEffect(key1 = Unit) {
								try {
									val settings =
										applicationSettingsRepository.promiseApplicationSettings().suspend()
									if (settings.chosenLibraryId > -1) {
										val libraryId = LibraryId(settings.chosenLibraryId)
										applicationNavigation.viewLibrary(libraryId).suspend()
										return@LaunchedEffect
									}
								} catch (e: Throwable) {
									logger.error("An error occurred initializing the library", e)
								}

								applicationNavigation.backOut().suspend()
							}
						}

						HiddenSettingsScreen -> {}
						is DownloadsScreen -> {}
						is ItemScreen -> {
							BrowserMediaView(
								viewModelDependencies = this,
								connectionStatusViewModel = connectionStatusViewModel,
								libraryId = destination.libraryId,
								item = destination.item,
							)
						}

						is LibraryScreen -> {
							BrowserMediaView(
								viewModelDependencies = this,
								connectionStatusViewModel = connectionStatusViewModel,
								libraryId = destination.libraryId,
								item = null,
							)
						}

						is SearchScreen -> {}
						is ConnectionSettingsScreen -> {
							TvLibrarySettingsView(
								librarySettingsViewModel = librarySettingsViewModel,
								navigateApplication = applicationNavigation,
								stringResources = stringResources,
							)

							librarySettingsViewModel.loadLibrary(destination.libraryId)
						}
						is NowPlayingScreen -> {}
						NewConnectionSettingsScreen -> {
							TvLibrarySettingsView(
								librarySettingsViewModel = librarySettingsViewModel,
								navigateApplication = applicationNavigation,
								stringResources = stringResources,
							)
						}
					}
				}
		}

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
		if (isCheckingConnection) {
			Box(modifier = Modifier.fillMaxSize()) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
