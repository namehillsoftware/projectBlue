package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.MainApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedBrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
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
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.settings.TvLibrarySettingsView
import com.lasthopesoftware.bluewater.settings.TvApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch

private val logger by lazyLogger<MainApplication>()

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun BrowserLibraryDestination.NowPlayingTvView(browserViewDependencies: ScopedBrowserViewDependencies) {
	val context = LocalContext.current
	LaunchedEffect(key1 = libraryId) {
		with (browserViewDependencies) {
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

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		val halfWidth = maxWidth / 2
		val halfWidthPx = LocalDensity.current.run { halfWidth.toPx() }

		var nowPlayingOffsetPx by remember { mutableFloatStateOf(halfWidthPx) }
		val nowPlayingOffset = LocalDensity.current.run { nowPlayingOffsetPx.toDp() }
		val nowPlayingWidth = maxWidth - nowPlayingOffset
		val browserOffset = nowPlayingOffset - halfWidth

		val isBrowserShown by remember { derivedStateOf { nowPlayingOffsetPx != 0f } }
		val browserExpansionProgress by remember { derivedStateOf { nowPlayingOffsetPx / halfWidthPx } }

		var currentPlaylistOffsetPx by remember { mutableFloatStateOf(0f) }

		val scope = rememberCoroutineScope()

		if (isBrowserShown) {
			Box(
				modifier = Modifier
					.offset(x = browserOffset)
					.width(halfWidth)
					.fillMaxHeight()
					.focusGroup()
			) {
				NavigateToTvLibraryDestination(browserViewDependencies)
			}
		} else {
			BackHandler {
				scope.launch {
					animate(0f, halfWidthPx, 0f, tween()) { value, _ ->
						currentPlaylistOffsetPx =
							(currentPlaylistOffsetPx - value - nowPlayingOffsetPx).coerceAtLeast(0f)
						nowPlayingOffsetPx = value
					}
				}
			}
		}

		Box(
			modifier = Modifier
				.width(nowPlayingWidth)
				.offset(x = nowPlayingOffset)
				.fillMaxHeight()
				.focusGroup()
		) {
			ControlSurface(
				color = Color.Transparent,
				contentColor = Color.White,
				controlColor = Color.White,
			) {
				NowPlayingCoverArtView(nowPlayingCoverArtViewModel = browserViewDependencies.nowPlayingCoverArtViewModel)

				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(SharedColors.overlayDark),
				) {
					with (browserViewDependencies) {
						BackHandler(itemListMenuBackPressedHandler.hideAllMenus()) {}

						val playlistExpansionProgress by remember { derivedStateOf { currentPlaylistOffsetPx / halfWidthPx } }
						val isPlaylistShown by remember { derivedStateOf { currentPlaylistOffsetPx != 0f } }
						val nowPlayingPaneWidth = this@BoxWithConstraints.maxWidth - LocalDensity.current.run { currentPlaylistOffsetPx.toDp() }

						Box(
							modifier = Modifier
								.fillMaxHeight()
								.width(nowPlayingPaneWidth)
								.focusGroup()
						) {
							NowPlayingHeadline(modifier = Modifier.fillMaxWidth(), nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel)

							NowPlayingControls(
								modifier = Modifier
									.align(Alignment.BottomCenter)
									.fillMaxWidth(),
								nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
								playbackServiceController = playbackServiceController,
							)

							Column(
								modifier = Modifier
									.align(Alignment.BottomCenter)
									.fillMaxWidth()
							) {
								Row(
									verticalAlignment = Alignment.CenterVertically
								) {
									val browserChevronRotation by remember { derivedStateOf { 90 - (180 * browserExpansionProgress) } }
									Image(
										painter = painterResource(R.drawable.chevron_up_white_36dp),
										alpha = playlistControlAlpha,
										contentDescription = stringResource(R.string.btn_hide_files),
										modifier = Modifier
											.navigable(onClick = {
												nowPlayingPlaylistViewModel.finishPlaylistEdit()
												scope.launch {
													val targetOffset = if (isBrowserShown) 0f else halfWidthPx
													val initialOffset = if (isBrowserShown) halfWidthPx else 0f
													animate(initialOffset, targetOffset, 0f, tween()) { value, _ ->
														currentPlaylistOffsetPx =
															(currentPlaylistOffsetPx - value - nowPlayingOffsetPx).coerceAtLeast(
																0f
															)
														nowPlayingOffsetPx = value
													}
												}
											})
											.rotate(browserChevronRotation),
									)

									NowPlayingRating(
										nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
										modifier = Modifier.weight(1f))

									val playlistChevronRotation by remember { derivedStateOf { -90 + (180 * playlistExpansionProgress) } }
									Image(
										painter = painterResource(R.drawable.chevron_up_white_36dp),
										alpha = playlistControlAlpha,
										contentDescription = stringResource(R.string.btn_hide_files),
										modifier = Modifier
											.navigable(
												onClick = {
													nowPlayingPlaylistViewModel.finishPlaylistEdit()
													scope.launch {
														val targetOffset = if (isPlaylistShown) 0f else halfWidthPx
														val initialOffset = if (isPlaylistShown) halfWidthPx else 0f
														animate(initialOffset, targetOffset, 0f, tween()) { value, _ ->
															nowPlayingOffsetPx =
																(nowPlayingOffsetPx - value - currentPlaylistOffsetPx).coerceAtLeast(
																	0f
																)
															currentPlaylistOffsetPx = value
														}
													}
												})
											.rotate(playlistChevronRotation),
									)
								}

								Spacer(modifier = Modifier.height(ProgressIndicatorDefaults.StrokeWidth))

								NowPlayingPlaybackControls(
									nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
									playbackServiceController = playbackServiceController,
								)
							}

							NowPlayingProgressIndicator(
								nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
								modifier = Modifier
									.fillMaxWidth()
									.align(Alignment.BottomCenter)
									.padding(bottom = controlRowHeight)
							)
						}

						DisposableEffect(key1 = isPlaylistShown) {
							if (!isPlaylistShown) {
								nowPlayingPlaylistViewModel.enableSystemAutoScrolling()
							} else {
								nowPlayingPlaylistViewModel.disableSystemAutoScrolling()
							}

							onDispose {  }
						}

						if (isPlaylistShown) {
							Column(
								modifier = Modifier
									.fillMaxHeight()
									.width(halfWidth)
									.offset(x = nowPlayingPaneWidth)
									.background(SharedColors.overlayDark),
								horizontalAlignment = Alignment.CenterHorizontally,
							) {
								PlaylistControls(
									modifier = Modifier
										.fillMaxWidth()
										.height(Dimensions.appBarHeight),
									playlistViewModel = nowPlayingPlaylistViewModel,
									viewModelMessageBus = nowPlayingViewModelMessageBus,
								)

								NowPlayingTvPlaylist(
									reusablePlaylistFileItemViewModelProvider,
									nowPlayingFilePropertiesViewModel,
									applicationNavigation,
									playbackServiceController,
									itemListMenuBackPressedHandler,
									nowPlayingPlaylistViewModel,
									viewModelMessageBus = nowPlayingViewModelMessageBus,
									modifier = Modifier
										.fillMaxHeight(),
								)
							}
						}
					}
				}
			}
		}
	}

	val connectionWatcherViewModel = browserViewDependencies.connectionWatcherViewModel
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
		when (this) {
			is BrowserLibraryDestination -> {
				NowPlayingTvView(browserViewDependencies = browserViewDependencies)
			}

			is ConnectionSettingsScreen -> {
				with(browserViewDependencies) {
					val viewModel = librarySettingsViewModel

					val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(systemBarsPadding)
					) {
						TvLibrarySettingsView(
							librarySettingsViewModel = viewModel,
							navigateApplication = applicationNavigation
						)
					}

					viewModel.loadLibrary(libraryId)
				}
			}

			is NowPlayingScreen -> {}
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
						TvApplicationSettingsView(
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
								TvLibrarySettingsView(
									librarySettingsViewModel = librarySettingsViewModel,
									navigateApplication = applicationNavigation
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
