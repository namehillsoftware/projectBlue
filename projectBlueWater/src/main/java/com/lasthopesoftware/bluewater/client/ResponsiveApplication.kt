package com.lasthopesoftware.bluewater.client

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.ScreenDimensionsScope
import com.lasthopesoftware.bluewater.android.ui.components.PaddedSystemScreenBox
import com.lasthopesoftware.bluewater.android.ui.findWindow
import com.lasthopesoftware.bluewater.android.ui.isNarrow
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.bottomSheetElevation
import com.lasthopesoftware.bluewater.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.android.ui.theme.isStatusBarLight
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.browsing.ReusedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsView
import com.lasthopesoftware.bluewater.client.browsing.files.list.search.SearchFilesView
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependentsRegistry
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.LoadedItemListView
import com.lasthopesoftware.bluewater.client.browsing.library.LibraryDestinationGraphNavigation
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibrarySearchScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowsedFileDetailsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.FileDetailsFromNowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.FilePropertySearchScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LandingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryMenu
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ResponsiveDestinationGraphNavigation
import com.lasthopesoftware.bluewater.client.browsing.navigation.RoutedNavigationDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchedFileDetailsScreen
import com.lasthopesoftware.bluewater.client.browsing.registerBackNav
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.libraries.RateLimitedFilePropertiesDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.RetryingLibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.KeepScreenOn
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingCoverArtView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingNarrowView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingWideView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.SavePlaylistDialog
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.minimumMenuWidth
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.exceptions.UncaughtExceptionHandlerLogger
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.observables.subscribeAsState
import com.lasthopesoftware.policies.ratelimiting.RateLimitingExecutionPolicy
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory
import java.io.IOException

private val logger by lazy { LoggerFactory.getLogger("ResponsiveApplication") }

enum class ResponsiveState { Browser, NowPlaying, Playlist }

@Composable
fun ScreenDimensionsScope.NavigateToBrowserLibraryDestination(
	destination: BrowserLibraryDestination,
	scopedDependencies: ScopedViewModelDependencies,
) {
	when (destination) {
		is LibraryScreen -> {
			LoadedItemListView(scopedDependencies, destination.libraryId, null)
		}

		is ItemScreen -> {
			LoadedItemListView(scopedDependencies, destination.libraryId, destination.item)
		}

		is DownloadsScreen -> {
			scopedDependencies.apply {
				ActiveFileDownloadsView(
					activeFileDownloadsViewModel = activeFileDownloadsViewModel,
					trackHeadlineViewModelProvider = reusableFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
				)

				activeFileDownloadsViewModel.loadActiveDownloads(destination.libraryId)
			}
		}

		is FilePropertySearchScreen,is SearchScreen -> {
			var isConnectionLost by remember { mutableStateOf(false) }
			var reinitializeConnection by remember { mutableStateOf(false) }

			scopedDependencies.apply {
				if (isConnectionLost) {
					ConnectionLostView(
						onCancel = { scopedDependencies.applicationNavigation.viewApplicationSettings() },
						onRetry = {
							reinitializeConnection = true
						}
					)
				} else {
					SearchFilesView(
						searchFilesViewModel = searchFilesViewModel,
						nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
						trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
						applicationNavigation = applicationNavigation,
						playbackServiceController = playbackServiceController,
						stringResources = stringResources,
						backStackBuilder = undoBackStackBuilder,
					)
				}

				ViewModelInitAction {
					searchFilesViewModel.setActiveLibraryId(destination.libraryId)

					if (reinitializeConnection) {
						LaunchedEffect(key1 = Unit) {
							isConnectionLost =
								!connectionStatusViewModel.initializeConnection(destination.libraryId).suspend()
							reinitializeConnection = false
						}
					}

					if (!isConnectionLost) {
						LaunchedEffect(destination) {
							try {
								when (destination) {
									is FilePropertySearchScreen -> {
										destination.filePropertyFilter?.let(searchFilesViewModel::prependFilter)
									}

									is SearchScreen -> {
										searchFilesViewModel.query.value = destination.searchQuery
									}
								}
								searchFilesViewModel.findFiles().suspend()

							} catch (e: IOException) {
								if (ConnectionLostExceptionFilter.isConnectionLostException(e))
									isConnectionLost = true
								else
									applicationNavigation.backOut().suspend()
							} catch (_: Exception) {
								applicationNavigation.backOut().suspend()
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun Navigate(destination: LibraryDestination, responsiveState: AnchoredDraggableState<ResponsiveState>, scopedViewModelDependencies: ScopedViewModelDependencies, permissionsDependencies: PermissionsDependencies): Unit = with(scopedViewModelDependencies) {
	when (destination) {
		is BrowserLibraryDestination, is NowPlayingScreen -> {
			val libraryId = destination.libraryId
			LaunchedEffect(key1 = libraryId) {
				try {
					val isConnectionActive = connectionWatcherViewModel.watchLibraryConnection(libraryId).suspend()

					if (isConnectionActive) {
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
							pollForConnections.pollConnection(libraryId)
						}

						UncaughtExceptionHandlerLogger.uncaughtException(e) -> {
							exceptionAnnouncer.announce(e)
						}
					}
				}
			}

			val browserNavController = rememberNavController<BrowserLibraryDestination>(LibraryScreen(libraryId))

			BoxWithConstraints(modifier = Modifier.fillMaxSize()) fullScreen@{
				findWindow()?.isStatusBarLight = false

				val paneWidth = if (isNarrow) maxWidth else maxHeight.coerceIn(minimumMenuWidth, maxWidth / 2)
				val paneWidthPx = LocalDensity.current.remember { paneWidth.toPx() }

				DisposableEffect(paneWidthPx, isNarrow) {
					responsiveState.updateAnchors(if (isNarrow) DraggableAnchors {
						ResponsiveState.NowPlaying at -paneWidthPx
						ResponsiveState.Browser at 0f
					} else DraggableAnchors {
						ResponsiveState.Playlist at 2 * -paneWidthPx
						ResponsiveState.NowPlaying at -paneWidthPx
						ResponsiveState.Browser at 0f
					})

					onDispose { }
				}

				val browserDrawerOffset by LocalDensity.current.remember(responsiveState) {
					derivedStateOf {
						responsiveState.requireOffset().toDp()
					}
				}

				val nowPlayingOffset by remember { derivedStateOf { (paneWidth + browserDrawerOffset).coerceAtLeast(0.dp) } }

				val playlistListState = rememberLazyListState()
				Box(
					modifier = Modifier
						.anchoredDraggable(
							responsiveState,
							orientation = Orientation.Horizontal,
						)
				) {
					val scope = rememberCoroutineScope()
					val browserNavDependencies = remember(scope, browserNavController) {
						object : ScopedViewModelDependencies by scopedViewModelDependencies {
							override val applicationNavigation by lazy {
								LibraryDestinationGraphNavigation(
									scopedViewModelDependencies.applicationNavigation,
									browserNavController,
									scope,
									itemListMenuBackPressedHandler
								)
							}
						}
					}

					browserNavDependencies.registerBackNav()

					NavHost(browserNavController) { browserDestination ->
						LocalViewModelStoreOwner.current
							?.let { viewModelStoreOwner ->
								ScopedViewModelRegistry(
									browserNavDependencies,
									permissionsDependencies,
									viewModelStoreOwner,
								)
							}
							?.registerBackNav()
							?.apply {
								val scaffoldState = rememberBottomSheetScaffoldState()
								Box(
									modifier = Modifier
										.offset { IntOffset(x = browserDrawerOffset.roundToPx(), y = 0) }
										.width(paneWidth)
										.fillMaxHeight()
										.focusGroup()
								) {
									if (this@fullScreen.isNarrow) {
										val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
										Column(
											modifier = Modifier
												.fillMaxSize()
												.padding(systemBarsPadding)
										) {
											val isBottomSheetCollapsed = scaffoldState.bottomSheetState.isCollapsed
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
												sheetPeekHeight = if (isSelectedLibrary) appBarHeight else 0.dp,
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
																nowPlayingFilePropertiesViewModel
																	.initializeViewModel(libraryId)
																	.suspend()
															} catch (e: Throwable) {
																when {
																	ConnectionLostExceptionFilter.isConnectionLostException(
																		e
																	) -> {
																		pollForConnections.pollConnection(libraryId)
																	}

																	UncaughtExceptionHandlerLogger.uncaughtException(
																		e
																	) -> {
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
														screenHeight = this@fullScreen.maxHeight,
														screenWidth = this@fullScreen.maxWidth,
														innerBoxScope = this@nestedBox
													)

													screenScope.NavigateToBrowserLibraryDestination(
														browserDestination,
														this@apply
													)
												}
											}
										}
									} else {
										Column {
											Spacer(
												modifier = Modifier
													.windowInsetsTopHeight(WindowInsets.systemBars)
													.fillMaxWidth()
											)

											BoxWithConstraints(
												modifier = Modifier
													.fillMaxWidth()
													.weight(1f)
											) {
												val screenScope = ScreenDimensionsScope(
													screenHeight = this@fullScreen.maxHeight,
													screenWidth = this@fullScreen.maxWidth,
													innerBoxScope = this
												)

												screenScope.NavigateToBrowserLibraryDestination(
													browserDestination,
													this@apply
												)
											}

											Spacer(
												modifier = Modifier
													.windowInsetsBottomHeight(WindowInsets.systemBars)
													.fillMaxWidth()
													.background(SharedColors.overlayDark)
											)
										}
									}
								}
							}
					}

					val isNowPlayingShown by remember { derivedStateOf { nowPlayingOffset < this@fullScreen.maxWidth } }
					if (isNowPlayingShown) {
						Box(
							modifier = Modifier
								.offset { IntOffset(x = nowPlayingOffset.roundToPx(), y = 0) }
								.fillMaxHeight()
								.focusGroup()
						) {
							val isScreenOn by nowPlayingScreenViewModel.isScreenOn.subscribeAsState()
							KeepScreenOn(isScreenOn)

							ControlSurface(
								color = Color.Transparent,
								contentColor = Color.White,
								controlColor = Color.White,
							) {
								NowPlayingCoverArtView(
									nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
									bitmapProducer = bitmapProducer,
								)

								Column {
									Spacer(
										modifier = Modifier
											.windowInsetsTopHeight(WindowInsets.systemBars)
											.fillMaxWidth()
											.background(SharedColors.overlayDark)
									)

									BoxWithConstraints(
										modifier = Modifier
											.fillMaxWidth()
											.weight(1f)
											.background(SharedColors.overlayDark),
									) nowPlayingPane@{
										if (isNarrow) {
											NowPlayingNarrowView(
												nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
												nowPlayingScreenViewModel = nowPlayingScreenViewModel,
												playbackServiceController = playbackServiceController,
												playlistViewModel = nowPlayingPlaylistViewModel,
												childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
												applicationNavigation = applicationNavigation,
												itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
												viewModelMessageBus = nowPlayingViewModelMessageBus,
												undoBackStack = undoBackStackBuilder,
												lazyListState = playlistListState,
											)
										} else {
											NowPlayingWideView(
												nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
												nowPlayingScreenViewModel = nowPlayingScreenViewModel,
												playbackServiceController = playbackServiceController,
												playlistViewModel = nowPlayingPlaylistViewModel,
												childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
												applicationNavigation = applicationNavigation,
												itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
												viewModelMessageBus = nowPlayingViewModelMessageBus,
												undoBackStack = undoBackStackBuilder,
												lazyListState = playlistListState,
												playlistDrawerState = responsiveState,
												closedState = ResponsiveState.NowPlaying,
												openState = ResponsiveState.Playlist,
											)
										}
									}

									Spacer(
										modifier = Modifier
											.windowInsetsBottomHeight(WindowInsets.systemBars)
											.fillMaxWidth()
											.background(SharedColors.overlayDark)
									)
								}
							}
						}

						SavePlaylistDialog(nowPlayingPlaylistViewModel)
					}
				}

				Spacer(
					modifier = Modifier
						.align(Alignment.TopStart)
						.windowInsetsTopHeight(WindowInsets.systemBars)
						.fillMaxWidth()
						.background(SharedColors.overlayDark)
				)

				Spacer(
					modifier = Modifier
						.align(Alignment.BottomStart)
						.windowInsetsBottomHeight(WindowInsets.systemBars)
						.fillMaxWidth()
						.background(SharedColors.overlayDark)
				)
			}

			val isConnectionLost by connectionWatcherViewModel.isCheckingConnection.subscribeAsState()
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

		is BrowsedFileDetailsScreen -> {
			val viewModel = browsedFileDetailsViewModel

			FileDetailsView(
				viewModel = viewModel,
				navigateApplication = applicationNavigation,
				bitmapProducer = bitmapProducer,
				playableFileDetailsState = viewModel,
			)

			destination.apply {
				viewModel.load(libraryId, item, positionedFile)
			}
		}

		is SearchedFileDetailsScreen -> {
			val viewModel = searchedFileDetailsViewModel

			FileDetailsView(
				viewModel = viewModel,
				navigateApplication = applicationNavigation,
				bitmapProducer = bitmapProducer,
				playableFileDetailsState = viewModel,
			)

			destination.apply {
				viewModel.load(libraryId, query, positionedFile)
			}
		}

		is FileDetailsFromNowPlayingScreen -> {
			val viewModel = nowPlayingFileDetailsViewModel

			FileDetailsView(
				viewModel = viewModel,
				navigateApplication = applicationNavigation,
				bitmapProducer = bitmapProducer,
				nowPlayingFileDetailsSate = viewModel,
				playableFileDetailsState = viewModel,
			)

			destination.apply {
				viewModel.load(libraryId, positionedFile)
			}
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
				viewModel.loadLibrary(destination.libraryId)
			}
		}
	}
}


@Composable
fun ResponsiveApplication(
	entryDependencies: EntryDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination?
) {
	val navController = rememberNavController(listOf(ApplicationSettingsScreen, LandingScreen))

	var browserDragValue by rememberSaveable {
		mutableStateOf(ResponsiveState.Browser)
	}

	val responsiveState = remember(browserDragValue) {
		AnchoredDraggableState(
			initialValue = browserDragValue,
			anchors = DraggableAnchors {
				ResponsiveState.Playlist at 2 * -100f
				ResponsiveState.NowPlaying at -100f
				ResponsiveState.Browser at 0f
			}
		)
	}

	val coroutineScope = rememberCoroutineScope()
	val destinationGraphNavigation = remember(entryDependencies, responsiveState, navController, coroutineScope) {
		ResponsiveDestinationGraphNavigation(
			entryDependencies.applicationNavigation,
			responsiveState,
			navController,
			coroutineScope,
			entryDependencies.itemListMenuBackPressedHandler
		)
	}

	val connectionStatusViewModel = viewModel {
		with (entryDependencies) {
			ConnectionStatusViewModel(
				stringResources,
				DramaticConnectionInitializationController(connectionSessions),
				registerForApplicationMessages,
			)
		}
	}

	val routedNavigationDependencies = remember(destinationGraphNavigation, connectionStatusViewModel, navController) {
		RoutedNavigationDependencies(
			entryDependencies,
			destinationGraphNavigation,
			connectionStatusViewModel,
			navController
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
		NavHost(navController) { destination ->
			when (destination) {
				is LandingScreen -> {
					LaunchedEffect(Unit) {
						(initialDestination
							?.let(routedNavigationDependencies::promiseNavigation)
							?: routedNavigationDependencies.applicationNavigation.viewActiveLibrary())
							.suspend()
					}
				}

				is ActiveLibrarySearchScreen -> {
					routedNavigationDependencies.apply {
						LaunchedEffect(Unit) {
							applicationNavigation.searchActiveLibrary(destination.searchQuery)
						}
					}
				}

				is ActiveLibraryDownloadsScreen -> {
					routedNavigationDependencies.apply {
						LaunchedEffect(key1 = Unit) {
							val selectedLibraryId = try {
								selectedLibraryIdProvider.promiseSelectedLibraryId().suspend()
							} catch (e: Throwable) {
								logger.error("An error occurred initializing the library", e)
								null
							}

							if (selectedLibraryId != null && selectedLibraryId.id > -1) {
								applicationNavigation.viewLibrary(selectedLibraryId).suspend()
								applicationNavigation.viewActiveDownloads(selectedLibraryId).suspend()
							} else {
								applicationNavigation.backOut().suspend()
							}
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
						?.also { Navigate(destination, responsiveState, it, permissionsDependencies) }
				}

				is ApplicationSettingsScreen -> {
					Box(
						modifier = Modifier.fillMaxSize()
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
