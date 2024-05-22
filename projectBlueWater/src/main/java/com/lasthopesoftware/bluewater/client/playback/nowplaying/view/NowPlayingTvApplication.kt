package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.MainApplication
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedBrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DestinationRoutingNavigation
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigateToTvLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.RoutedNavigationDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch

private val logger by lazyLogger<MainApplication>()

@ExperimentalFoundationApi
@Composable
private fun ScreenDimensionsScope.NowPlayingTvOverlay(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
) {
	BackHandler(itemListMenuBackPressedHandler.hideAllMenus()) {}

	val playlistWidth = screenHeight.coerceAtMost(screenWidth / 2)
	val playlistWidthPx = LocalDensity.current.run { playlistWidth.toPx() }

	var isPlaylistShown by remember { mutableStateOf(false) }
	var currentPlaylistOffsetPx by remember { mutableFloatStateOf(0f) }
	val playlistExpansionProgress by remember { derivedStateOf { currentPlaylistOffsetPx / playlistWidthPx } }
	val nowPlayingPaneWidth = screenWidth - LocalDensity.current.run { currentPlaylistOffsetPx.toDp() }

	Box(
		modifier = Modifier.fillMaxSize(),
	) {
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.width(nowPlayingPaneWidth)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				NowPlayingHeadline(modifier = Modifier.weight(1f), nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel)

				val scope = rememberCoroutineScope()
				val chevronRotation by remember { derivedStateOf { -90 + (180 * playlistExpansionProgress) } }
				Image(
					painter = painterResource(R.drawable.chevron_up_white_36dp),
					alpha = playlistControlAlpha,
					contentDescription = stringResource(R.string.btn_hide_files),
					modifier = Modifier
						.clickable(onClick = {
							playlistViewModel.finishPlaylistEdit()
							scope.launch {
								val targetOffset = if (isPlaylistShown) 0f else playlistWidthPx
								val initialOffset = if (isPlaylistShown) playlistWidthPx else 0f
								animate(initialOffset, targetOffset, 0f, tween()) { value, _ ->
									currentPlaylistOffsetPx = value
									isPlaylistShown = value != 0f
								}
							}
						})
						.rotate(chevronRotation),
				)
			}

			NowPlayingControls(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth(),
				nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
				playbackServiceController = playbackServiceController,
			)

			NowPlayingProgressIndicator(
				nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.padding(bottom = controlRowHeight)
			)
		}

		Column(
			modifier = Modifier
				.fillMaxHeight()
				.width(playlistWidth)
				.offset(x = nowPlayingPaneWidth)
				.background(SharedColors.overlayDark),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			if (!isPlaylistShown) {
				playlistViewModel.enableSystemAutoScrolling()
			} else {
				playlistViewModel.disableSystemAutoScrolling()
			}

			PlaylistControls(
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimensions.appBarHeight),
				playlistViewModel = playlistViewModel,
				viewModelMessageBus = viewModelMessageBus,
			)

			NowPlayingTvPlaylist(
				childItemViewModelProvider,
				nowPlayingFilePropertiesViewModel,
				applicationNavigation,
				playbackServiceController,
				itemListMenuBackPressedHandler,
				playlistViewModel,
				viewModelMessageBus = viewModelMessageBus,
				modifier = Modifier
					.fillMaxHeight()
					.onFocusChanged { f ->
						if (f.hasFocus) playlistViewModel.lockOutAutoScroll()
						else playlistViewModel.releaseAutoScroll()
					},
			)
		}
	}
}

@ExperimentalFoundationApi
@Composable
fun NowPlayingTvView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	connectionWatcherViewModel: ConnectionWatcherViewModel,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>
) {
	ControlSurface(
		color = Color.Transparent,
		contentColor = Color.White,
		controlColor = Color.White,
	) {
		NowPlayingCoverArtView(nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel)

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

		// Nest boxes to get proper size constraints
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.background(SharedColors.overlayDark)
				.padding(systemBarsPadding),
		) {
			val screenScope by remember {
				derivedStateOf {
					with (this@BoxWithConstraints) {
						ScreenDimensionsScope(
							maxHeight + systemBarsPadding.calculateTopPadding() + systemBarsPadding.calculateBottomPadding(),
							maxWidth  + systemBarsPadding.calculateLeftPadding(LayoutDirection.Ltr) + systemBarsPadding.calculateRightPadding(
								LayoutDirection.Ltr),
							this
						)
					}
				}
			}

			with (screenScope) {
				NowPlayingTvOverlay(
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					playbackServiceController = playbackServiceController,
					playlistViewModel = playlistViewModel,
					childItemViewModelProvider = childItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					viewModelMessageBus = viewModelMessageBus,
				)
			}
		}
	}

	val isConnectionLost by connectionWatcherViewModel.isCheckingConnection.collectAsState()
	if (isConnectionLost) {
		AlertDialog(
			onDismissRequest = { connectionWatcherViewModel.cancelLibraryConnectionPolling() },
			title = { Text(text = stringResource(id = R.string.lbl_connection_lost_title)) },
			text = {
				Text(
					text = stringResource(
						id = R.string.lbl_attempting_to_reconnect,
						stringResource(id = R.string.app_name)
					)
				)
			},
			buttons = {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(Dimensions.viewPaddingUnit),
					horizontalArrangement = Arrangement.Center,
				) {
					Button(
						onClick = {
							connectionWatcherViewModel.cancelLibraryConnectionPolling()
						},
					) {
						Text(text = stringResource(id = R.string.btn_cancel))
					}
				}
			},
			properties = DialogProperties(
				dismissOnBackPress = true,
			)
		)
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LibraryDestination.Navigate(browserViewDependencies: ScopedBrowserViewDependencies) {
	with(browserViewDependencies) {
		when (this@Navigate) {
			is BrowserLibraryDestination -> {
				Row(modifier = Modifier.fillMaxSize()) {
					Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.focusGroup()
					) {
						NavigateToTvLibraryDestination(browserViewDependencies)
					}

					Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.focusGroup()
							.focusable(true)
					) {
						NowPlayingTvView(
							nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
							nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
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
								connectionWatcherViewModel.watchLibraryConnection(libraryId)

								Promise.whenAll(
									nowPlayingScreenViewModel.initializeViewModel(libraryId),
									nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId),
									nowPlayingCoverArtViewModel.initializeViewModel(libraryId),
									nowPlayingPlaylistViewModel.initializeView(libraryId),
								).suspend()
							} catch (e: Throwable) {
								if (ConnectionLostExceptionFilter.isConnectionLostException(e))
									pollForConnections.pollConnection(libraryId)
								else
									UnexpectedExceptionToaster.announce(context, e)
							}
						}
					}
				}
			}

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

			is NowPlayingScreen -> {}
		}
	}
}

@Composable
fun NowPlayingTvApplication(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination?
) {
	val systemUiController = rememberSystemUiController()

	val navController = rememberNavController(
		if (initialDestination == null) listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
		else listOf(ApplicationSettingsScreen)
	)

	val coroutineScope = rememberCoroutineScope()

	val destinationRoutingNavigation = remember {
		SkipNowPlayingNavigation(
			DestinationRoutingNavigation(
				browserViewDependencies.applicationNavigation,
				navController,
				coroutineScope,
				browserViewDependencies.itemListMenuBackPressedHandler
			)
		)
	}

	val connectionStatusViewModel = viewModel {
		ConnectionStatusViewModel(
			browserViewDependencies.stringResources,
			DramaticConnectionInitializationController(
				browserViewDependencies.libraryConnectionProvider,
				destinationRoutingNavigation,
			),
		)
	}

	val routedNavigationDependencies = remember {
		RoutedNavigationDependencies(
			browserViewDependencies,
			destinationRoutingNavigation,
			connectionStatusViewModel,
			navController,
			initialDestination
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
							ScopedViewModelDependencies(routedNavigationDependencies, permissionsDependencies, it)
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
							ScopedViewModelDependencies(routedNavigationDependencies, permissionsDependencies, it)
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

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
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
