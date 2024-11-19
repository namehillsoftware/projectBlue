package com.lasthopesoftware.bluewater.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.LibraryConnectedDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.ProjectBlueApplication
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.browsing.ReusedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DestinationRoutingNavigation
import com.lasthopesoftware.bluewater.client.browsing.navigation.FileDetailsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryMenu
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigateToLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.RoutedNavigationDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependencies
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingView
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val logger by lazyLogger<ProjectBlueApplication>()

private class BottomSheetHidingNavigation(
	private val inner: NavigateApplication,
	private val bottomSheetState: BottomSheetState,
	private val coroutineScope: CoroutineScope,
	private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) : NavigateApplication by inner {

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.launchSearch(libraryId)
	}

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> {
		hideBottomSheet()

		return inner.search(libraryId, filePropertyFilter)
	}

	override fun viewServerSettings(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewServerSettings(libraryId)
	}

	override fun viewActiveDownloads(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewActiveDownloads(libraryId)
	}

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewLibrary(libraryId)
	}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> {
		hideBottomSheet()

		return inner.viewItem(libraryId, item)
	}

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		hideBottomSheet()
		return inner.viewFileDetails(libraryId, playlist, position)
	}

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewNowPlaying(libraryId)
	}

	override fun navigateUp(): Promise<Boolean> {
		hideBottomSheet()

		return inner.navigateUp()
	}

	override fun backOut(): Promise<Boolean> {
		val isHidden = itemListMenuBackPressedHandler.hideAllMenus() or hideBottomSheet()

		return if (isHidden) true.toPromise()
		else inner.backOut()
	}

	private fun hideBottomSheet(): Boolean {
		if (!bottomSheetState.isCollapsed) {
			coroutineScope.launch { bottomSheetState.collapse() }
			return true
		}

		return false
	}
}

private val bottomAppBarHeight = Dimensions.appBarHeight
private val bottomSheetElevation = 16.dp

@Composable
private fun BrowserLibraryDestination.Navigate(
	browserViewDependencies: ScopedViewModelDependencies,
	libraryConnectionDependencies: LibraryConnectionDependencies,
	scaffoldState: BottomSheetScaffoldState,
) {
	with(browserViewDependencies) {
		val selectedLibraryId by selectedLibraryViewModel.selectedLibraryId.collectAsState()
		val isSelectedLibrary by remember { derivedStateOf { selectedLibraryId == libraryId } }

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
		BottomSheetScaffold(
			modifier = Modifier
				.fillMaxSize()
				.padding(systemBarsPadding),
			scaffoldState = scaffoldState,
			sheetPeekHeight = if (isSelectedLibrary) bottomAppBarHeight else 0.dp,
			sheetElevation = bottomSheetElevation,
			sheetContent = {
				if (isSelectedLibrary) {
					LibraryMenu(
						applicationNavigation = applicationNavigation,
						nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
						playbackServiceController = playbackServiceController,
						bottomSheetState = scaffoldState.bottomSheetState,
						libraryId = libraryId,
					)

					val context = LocalContext.current
					LaunchedEffect(key1 = libraryId) {
						try {
							nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId).suspend()
						} catch (e: Throwable) {
							if (ConnectionLostExceptionFilter.isConnectionLostException(e))
								libraryConnectionDependencies.pollForConnections.pollConnection(libraryId)
							else
								UnexpectedExceptionToaster.announce(context, e)
						}
					}
				}
			}
		) { paddingValues ->
			Box(modifier = Modifier.padding(paddingValues)) { NavigateToLibraryDestination(browserViewDependencies) }
		}
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LibraryDestination.Navigate(
	browserViewDependencies: ScopedViewModelDependencies,
	libraryConnectionDependencies: LibraryConnectionDependencies,
	scaffoldState: BottomSheetScaffoldState,
) {
	with(browserViewDependencies) {
		when (this@Navigate) {
			is BrowserLibraryDestination -> {
				Navigate(
					browserViewDependencies = browserViewDependencies,
					libraryConnectionDependencies = libraryConnectionDependencies,
					scaffoldState = scaffoldState,
				)
			}

			is FileDetailsScreen -> {}

			is ConnectionSettingsScreen -> {
				val viewModel = librarySettingsViewModel

				val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(systemBarsPadding)
				) {
					LibrarySettingsView(
						librarySettingsViewModel = viewModel,
						navigateApplication = applicationNavigation,
						stringResources = stringResources,
						userSslCertificates = userSslCertificateProvider,
					)
				}

				viewModel.loadLibrary(libraryId)
			}

			is NowPlayingScreen -> {
				val systemUiController = rememberSystemUiController()
				systemUiController.setSystemBarsColor(SharedColors.overlayDark)

				NowPlayingView(
					nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					screenOnState = nowPlayingScreenViewModel,
					playbackServiceController = playbackServiceController,
					playlistViewModel = nowPlayingPlaylistViewModel,
					childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					connectionWatcherViewModel = connectionWatcherViewModel,
					viewModelMessageBus = nowPlayingViewModelMessageBus
				)

				val context = LocalContext.current
				LaunchedEffect(key1 = libraryId) {
					try {
						if (!connectionStatusViewModel.initializeConnection(libraryId).suspend()) {
							return@LaunchedEffect
						}

						if (!connectionWatcherViewModel.watchLibraryConnection(libraryId).suspend()) {
							return@LaunchedEffect
						}

						Promise.whenAll(
							nowPlayingScreenViewModel.initializeViewModel(libraryId),
							nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId),
							nowPlayingCoverArtViewModel.initializeViewModel(libraryId),
							nowPlayingPlaylistViewModel.initializeView(libraryId),
						).suspend()
					} catch (e: Throwable) {
						if (ConnectionLostExceptionFilter.isConnectionLostException(e))
							libraryConnectionDependencies.pollForConnections.pollConnection(libraryId)
						else
							UnexpectedExceptionToaster.announce(context, e)
					}
				}
			}
		}
	}
}

@Composable
fun HandheldApplication(
	entryDependencies: EntryDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination?
) {
	val systemUiController = rememberSystemUiController()

	val navController = rememberNavController(
		if (initialDestination == null) listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
		else listOf(ApplicationSettingsScreen)
	)
	val scaffoldState = rememberBottomSheetScaffoldState()
	val coroutineScope = rememberCoroutineScope()

	val bottomSheetState = scaffoldState.bottomSheetState
	val destinationRoutingNavigation = remember {
		BottomSheetHidingNavigation(
			DestinationRoutingNavigation(
				entryDependencies.applicationNavigation,
				navController,
				coroutineScope,
				entryDependencies.itemListMenuBackPressedHandler
			),
			bottomSheetState,
			coroutineScope,
			entryDependencies.itemListMenuBackPressedHandler
		)
	}

	val connectionStatusViewModel = viewModel {
		ConnectionStatusViewModel(
			entryDependencies.stringResources,
			DramaticConnectionInitializationController(
				entryDependencies.connectionSessions,
				destinationRoutingNavigation,
			),
		)
	}

	val routedNavigationDependencies = remember {
		RoutedNavigationDependencies(
			entryDependencies,
			destinationRoutingNavigation,
			connectionStatusViewModel,
			navController,
			initialDestination
		)
	}

	val libraryConnectionDependencies = remember {
		LibraryConnectedDependencies(routedNavigationDependencies)
	}

	val viewModelStoreOwner = LocalViewModelStoreOwner.current ?: return
	val reusedViewModelDependencies = remember {
		ReusedViewModelRegistry(
			routedNavigationDependencies,
			libraryConnectionDependencies,
			viewModelStoreOwner
		)
	}

	DisposableEffect(key1 = routedNavigationDependencies) {
		onDispose {
			routedNavigationDependencies.close()
		}
	}

	BackHandler { routedNavigationDependencies.applicationNavigation.backOut() }

	val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

	ControlSurface {
		NavHost(navController) { destination ->
			systemUiController.setStatusBarColor(MaterialTheme.colors.surface)
			systemUiController.setNavigationBarColor(Color.Black)

			when (destination) {
				is SelectedLibraryReRouter -> {
					routedNavigationDependencies.apply {
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
				is ActiveLibraryDownloadsScreen -> {
					routedNavigationDependencies.apply {
						LaunchedEffect(key1 = Unit) {
							try {
								val settings =
									applicationSettingsRepository.promiseApplicationSettings().suspend()
								if (settings.chosenLibraryId > -1) {
									val libraryId = LibraryId(settings.chosenLibraryId)
									applicationNavigation.viewLibrary(libraryId).suspend()
									applicationNavigation.viewActiveDownloads(libraryId).suspend()
									return@LaunchedEffect
								}
							} catch (e: Throwable) {
								logger.error("An error occurred initializing the library", e)
							}

							applicationNavigation.backOut().suspend()
						}
					}
				}
				is LibraryDestination -> {
					LocalViewModelStoreOwner.current?.also {
						destination.Navigate(
							ScopedViewModelRegistry(
								reusedViewModelDependencies,
								permissionsDependencies,
								it
							),
							libraryConnectionDependencies,
							scaffoldState
						)
					}
				}
				is ApplicationSettingsScreen -> {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(systemBarsPadding)
					) {
						ApplicationSettingsView(
							applicationSettingsViewModel = routedNavigationDependencies.applicationSettingsViewModel,
							applicationNavigation = routedNavigationDependencies.applicationNavigation,
							playbackService = routedNavigationDependencies.playbackServiceController,
						)
					}

					routedNavigationDependencies.applicationSettingsViewModel.loadSettings()
				}
				is NewConnectionSettingsScreen -> {
					LocalViewModelStoreOwner.current
						?.let {
							ScopedViewModelRegistry(
								reusedViewModelDependencies,
								permissionsDependencies,
								it
							)
						}
						?.apply {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.padding(systemBarsPadding)
							) {
								LibrarySettingsView(
									librarySettingsViewModel = librarySettingsViewModel,
									navigateApplication = applicationNavigation,
									stringResources = stringResources,
									userSslCertificates = userSslCertificateProvider,
								)
							}
						}
				}
				is HiddenSettingsScreen -> {
					HiddenSettingsView(routedNavigationDependencies.hiddenSettingsViewModel)

					routedNavigationDependencies.hiddenSettingsViewModel.loadApplicationSettings()
				}
			}
		}

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.subscribeAsState()
		if (isCheckingConnection) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(systemBarsPadding)
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
