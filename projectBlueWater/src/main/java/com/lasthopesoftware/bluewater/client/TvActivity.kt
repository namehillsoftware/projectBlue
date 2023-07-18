package com.lasthopesoftware.bluewater.client

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.TvItemView
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
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializingLibrarySelectionNavigation
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.settings.TvLibrarySettingsView
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.TvApplicationSettingsView
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
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
	private val navController: NavController<Destination>,
	private val coroutineScope: CoroutineScope
) : NavigateApplication {
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

	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(SearchScreen(libraryId))
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		if (item is Item)
			navController.navigate(ItemScreen(libraryId, item))
	}.toPromise()

//	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
//		return inner.viewFileDetails(libraryId, playlist, position)
//	}

	override fun viewNowPlaying(libraryId: LibraryId) = coroutineScope.launch {
		if (!navController.moveToTop { it is NowPlayingScreen }) {
			navController.navigate(NowPlayingScreen(libraryId))
		}
	}.toPromise()
}

private class TvDependencies(
	private val inner: BrowserViewDependencies,
	private val tvNavigation: TvNavigation,
	private val connectionStatusViewModel: ConnectionStatusViewModel,
) : BrowserViewDependencies by inner {
	override val applicationNavigation by lazy {
		ConnectionInitializingLibrarySelectionNavigation(
			tvNavigation,
			selectedLibraryViewModel,
			connectionStatusViewModel,
		)
	}
}

@Composable
fun CatalogBrowser(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies
) {
	val navController = rememberNavController(
		listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
	)

	val coroutineScope = rememberCoroutineScope()

	val graphNavigation = remember {
		TvNavigation(navController, coroutineScope)
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

	val tvDependencies =
		remember { TvDependencies(browserViewDependencies, graphNavigation, connectionStatusViewModel) }

	BackHandler { tvDependencies.applicationNavigation.backOut() }

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
						tvDependencies.apply {
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
					}

					HiddenSettingsScreen -> {}
					is DownloadsScreen -> {}
					is ItemScreen -> {
						TvItemView(
							itemListViewModel = itemListViewModel,
							fileListViewModel = fileListViewModel,
							navigateApplication = applicationNavigation,
							trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
						)

						val (libraryId, item) = destination

						var isConnectionLost by remember { mutableStateOf(false) }
						var reinitializeConnection by remember { mutableStateOf(false) }

						if (isConnectionLost) {
							ConnectionLostView(
								onCancel = { applicationNavigation.viewApplicationSettings() },
								onRetry = {
									reinitializeConnection = true
								}
							)
						} else {
							TvItemView(
								itemListViewModel = itemListViewModel,
								fileListViewModel = fileListViewModel,
								navigateApplication = applicationNavigation,
								trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
							)
						}

						if (reinitializeConnection) {
							LaunchedEffect(key1 = Unit) {
								isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
								reinitializeConnection = false
							}
						}

						if (!isConnectionLost) {
							LaunchedEffect(libraryId) {
								try {
									Promise.whenAll(
										itemListViewModel.loadItem(libraryId, item),
										fileListViewModel.loadItem(libraryId, item),
									).suspend()
								} catch (e: IOException) {
									if (ConnectionLostExceptionFilter.isConnectionLostException(e))
										isConnectionLost = true
									else
										applicationNavigation.backOut().suspend()
								} catch (e: Exception) {
									applicationNavigation.backOut().suspend()
								}
							}
						}
					}

					is LibraryScreen -> {
						val libraryId = destination.libraryId

						var isConnectionLost by remember { mutableStateOf(false) }
						var reinitializeConnection by remember { mutableStateOf(false) }

						if (isConnectionLost) {
							ConnectionLostView(
								onCancel = { applicationNavigation.viewApplicationSettings() },
								onRetry = {
									reinitializeConnection = true
								}
							)
						} else {
							TvItemView(
								itemListViewModel = itemListViewModel,
								fileListViewModel = fileListViewModel,
								navigateApplication = applicationNavigation,
								trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
							)
						}

						if (reinitializeConnection) {
							LaunchedEffect(key1 = Unit) {
								isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
								reinitializeConnection = false
							}
						}

						if (!isConnectionLost) {
							LaunchedEffect(libraryId) {
								try {
									Promise.whenAll(
										itemListViewModel.loadItem(libraryId),
										fileListViewModel.loadItem(libraryId),
									).suspend()
								} catch (e: IOException) {
									if (ConnectionLostExceptionFilter.isConnectionLostException(e))
										isConnectionLost = true
									else
										applicationNavigation.backOut().suspend()
								} catch (e: Exception) {
									applicationNavigation.backOut().suspend()
								}
							}
						}
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
		Box(
			modifier = Modifier.fillMaxSize()
		) {
			ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
		}
	}
}
