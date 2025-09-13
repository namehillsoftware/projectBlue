package com.lasthopesoftware.bluewater.client

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.android.ui.components.PaddedSystemScreenBox
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.DetermineWindowControlColors
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.browsing.ReusedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsView
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependentsRegistry
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DestinationGraphNavigation
import com.lasthopesoftware.bluewater.client.browsing.navigation.FileDetailsFromNowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryMenu
import com.lasthopesoftware.bluewater.client.browsing.navigation.ListedFileDetailsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigateToLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.RoutedNavigationDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.browsing.registerBackNav
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependents
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.libraries.RateLimitedFilePropertiesDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.RetryingLibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.ScreenDimensionsScope
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.exceptions.UncaughtExceptionHandlerLogger
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.policies.ratelimiting.RateLimitingExecutionPolicy
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory

private val logger by lazy { LoggerFactory.getLogger("HandheldApplication") }

private val bottomAppBarHeight = Dimensions.appBarHeight
private val bottomSheetElevation = 16.dp

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun BrowserLibraryDestination.Navigate(
	browserViewDependencies: ScopedViewModelDependencies,
	libraryConnectionDependencies: LibraryConnectionDependents,
) {
	with(browserViewDependencies) {
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black)
		) screenBox@{

			val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
			val layoutDirection = LocalLayoutDirection.current
			Column(
				modifier = Modifier
					.padding(
						start = systemBarsPadding.calculateStartPadding(layoutDirection),
						end = systemBarsPadding.calculateEndPadding(layoutDirection),
						bottom = systemBarsPadding.calculateBottomPadding(),
					)
					.fillMaxSize()
			) {
				Spacer(
					modifier = Modifier
						.windowInsetsTopHeight(WindowInsets.systemBars)
						.fillMaxWidth()
						.background(MaterialTheme.colors.surface)
				)

				val scaffoldState = rememberBottomSheetScaffoldState()

				val isBottomSheetCollapsed = scaffoldState.bottomSheetState.isCollapsed
				val scope = rememberCoroutineScope()
				DisposableEffect(isBottomSheetCollapsed) {
					if (isBottomSheetCollapsed) {
						onDispose { }
					} else {
						val collapseAction = {
							scope.async {
								if (scaffoldState.bottomSheetState.isCollapsed) false
								else {
									scaffoldState.bottomSheetState.collapse()
									true
								}
							}.toPromise()
						}

						undoBackStackBuilder.addAction(collapseAction)

						onDispose { undoBackStackBuilder.removeAction(collapseAction) }
					}
				}
				val selectedLibraryId by selectedLibraryViewModel.selectedLibraryId.subscribeAsState()
				val isSelectedLibrary by remember { derivedStateOf { selectedLibraryId == libraryId } }

				BottomSheetScaffold(
					modifier = Modifier.weight(1f),
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

							LaunchedEffect(key1 = libraryId) {
								try {
									nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId).suspend()
								} catch (e: Throwable) {
									when {
										ConnectionLostExceptionFilter.isConnectionLostException(e) -> {
											libraryConnectionDependencies.pollForConnections.pollConnection(libraryId)
										}

										UncaughtExceptionHandlerLogger.uncaughtException(e) -> {
											exceptionAnnouncer.announce(e)
										}
									}
								}
							}
						}
					}
				) { paddingValues ->
					BoxWithConstraints(modifier = Modifier.padding(paddingValues)) nestedBox@{
						val screenScope = ScreenDimensionsScope(
							screenHeight = this@screenBox.maxHeight,
							screenWidth = this@screenBox.maxWidth,
							innerBoxScope = this@nestedBox
						)

						screenScope.NavigateToLibraryDestination(
							this@Navigate,
							browserViewDependencies
						)
					}
				}
			}
		}
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LibraryDestination.Navigate(
	browserViewDependencies: ScopedViewModelDependencies,
	libraryConnectionDependencies: LibraryConnectionDependents,
) {
	with(browserViewDependencies) {
		when (this@Navigate) {

			is BrowserLibraryDestination -> {
				Navigate(
					browserViewDependencies = browserViewDependencies,
					libraryConnectionDependencies = libraryConnectionDependencies,
				)
			}

			is ListedFileDetailsScreen -> {
				val viewModel = listedFileDetailsViewModel

				FileDetailsView(
					viewModel = viewModel,
					navigateApplication = applicationNavigation,
					bitmapProducer = bitmapProducer,
					playableFileDetailsState = viewModel,
				)

				viewModel.load(libraryId, files, position)
			}

			is FileDetailsFromNowPlayingScreen -> {
				val viewModel = fileDetailsFromNowPlayingViewModel

				FileDetailsView(
					viewModel = viewModel,
					navigateApplication = applicationNavigation,
					bitmapProducer = bitmapProducer,
					playableFileDetailsState = viewModel,
				)

				viewModel.load(libraryId, positionedFile)
			}

			is ConnectionSettingsScreen -> {
				PaddedSystemScreenBox {
					val viewModel = librarySettingsViewModel

					LibrarySettingsView(
						librarySettingsViewModel = viewModel,
						navigateApplication = applicationNavigation,
						stringResources = stringResources,
						userSslCertificates = userSslCertificateProvider,
						undoBackStack = undoBackStackBuilder,
					)

					viewModel.loadLibrary(libraryId)
				}
			}

			is NowPlayingScreen -> {
				NowPlayingView(
					nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					nowPlayingScreenViewModel = nowPlayingScreenViewModel,
					playbackServiceController = playbackServiceController,
					playlistViewModel = nowPlayingPlaylistViewModel,
					childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					connectionWatcherViewModel = connectionWatcherViewModel,
					viewModelMessageBus = nowPlayingViewModelMessageBus,
					bitmapProducer = bitmapProducer,
					undoBackStack = undoBackStackBuilder,
				)

				LaunchedEffect(key1 = libraryId) {
					try {
						if (connectionWatcherViewModel.watchLibraryConnection(libraryId).suspend()) {
							Promise.whenAll(
								nowPlayingScreenViewModel.initializeViewModel(libraryId),
								nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId),
								nowPlayingCoverArtViewModel.initializeViewModel(libraryId),
								nowPlayingPlaylistViewModel.initializeView(libraryId),
							).suspend()
						}
					} catch (e: Throwable) {
						when {
							ConnectionLostExceptionFilter.isConnectionLostException(e) -> {
								libraryConnectionDependencies.pollForConnections.pollConnection(libraryId)
							}

							UncaughtExceptionHandlerLogger.uncaughtException(e) -> {
								exceptionAnnouncer.announce(e)
							}
						}
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
	val navController = rememberNavController(
		if (initialDestination == null) listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
		else listOf(ApplicationSettingsScreen)
	)

	val coroutineScope = rememberCoroutineScope()
	val destinationGraphNavigation = remember(navController, coroutineScope) {
		DestinationGraphNavigation(
			entryDependencies.applicationNavigation,
			navController,
			coroutineScope,
			entryDependencies.itemListMenuBackPressedHandler
		)
	}

	val connectionStatusViewModel = viewModel {
		with(entryDependencies) {
			ConnectionStatusViewModel(
				stringResources,
				DramaticConnectionInitializationController(
					connectionSessions,
				),
				registerForApplicationMessages,
			)
		}
	}

	val routedNavigationDependencies = remember(destinationGraphNavigation, connectionStatusViewModel, navController) {
		RoutedNavigationDependencies(
			entryDependencies,
			destinationGraphNavigation,
			connectionStatusViewModel,
			navController,
			initialDestination
		)
	}

	val libraryConnectionDependencies = remember(routedNavigationDependencies) {
		RateLimitedFilePropertiesDependencies(
			RateLimitingExecutionPolicy(1),
			RetryingLibraryConnectionRegistry(
				LibraryConnectionRegistry(routedNavigationDependencies),
			),
		)
	}

	val viewModelStoreOwner = LocalViewModelStoreOwner.current ?: return
	val reusedViewModelDependencies = remember(routedNavigationDependencies, libraryConnectionDependencies) {
		ReusedViewModelRegistry(
			routedNavigationDependencies,
			libraryConnectionDependencies,
			LibraryFilePropertiesDependentsRegistry(routedNavigationDependencies, libraryConnectionDependencies),
			viewModelStoreOwner
		)
	}

	DisposableEffect(key1 = routedNavigationDependencies) {
		onDispose {
			routedNavigationDependencies.close()
		}
	}

	routedNavigationDependencies.registerBackNav()

	ControlSurface {
		DetermineWindowControlColors()

		NavHost(navController) { destination ->
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
					LocalViewModelStoreOwner.current
						?.let { viewModelStoreOwner ->
							ScopedViewModelRegistry(
								reusedViewModelDependencies,
								permissionsDependencies,
								viewModelStoreOwner,
							)
						}
						?.registerBackNav()
						?.also { registry ->
							destination.Navigate(
								registry,
								libraryConnectionDependencies
							)
						}
				}

				is ApplicationSettingsScreen -> {
					routedNavigationDependencies.apply {
						ApplicationSettingsView(
							applicationSettingsViewModel = applicationSettingsViewModel,
							applicationNavigation = applicationNavigation,
							playbackService = playbackServiceController,
						)

						applicationSettingsViewModel.loadSettings()
					}
				}

				is NewConnectionSettingsScreen -> {
					LocalViewModelStoreOwner.current
						?.let { viewModelStoreOwner ->
							ScopedViewModelRegistry(
								reusedViewModelDependencies,
								permissionsDependencies,
								viewModelStoreOwner,
							)
						}
						?.registerBackNav()
						?.apply {
							PaddedSystemScreenBox {
								LibrarySettingsView(
									librarySettingsViewModel = librarySettingsViewModel,
									navigateApplication = applicationNavigation,
									stringResources = stringResources,
									userSslCertificates = userSslCertificateProvider,
									undoBackStack = undoBackStackBuilder,
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
				modifier = Modifier.fillMaxSize()
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
