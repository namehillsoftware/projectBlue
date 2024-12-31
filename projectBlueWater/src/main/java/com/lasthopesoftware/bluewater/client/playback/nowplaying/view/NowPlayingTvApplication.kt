package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.ProjectBlueApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.RateLimitedFilePropertiesDependencies
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.browsing.ReusedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelRegistry
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
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
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.RoutedNavigationDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.RetryingConnectionApplicationDependencies
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.NowPlayingTvItemView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.settings.TvLibrarySettingsView
import com.lasthopesoftware.bluewater.settings.TvApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.ui.SlideOutState
import com.lasthopesoftware.bluewater.shared.android.ui.calculateProgress
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.policies.ratelimiting.RateLimitingExecutionPolicy
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch

private val logger by lazyLogger<ProjectBlueApplication>()

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun BrowserLibraryDestination.NowPlayingTvView(browserViewDependencies: ScopedViewModelDependencies) {
	val context = LocalContext.current
	LaunchedEffect(key1 = libraryId) {
		with (browserViewDependencies) {
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
				if (ConnectionLostExceptionFilter.isConnectionLostException(e))
					pollForConnections.pollConnection(libraryId)
				else
					UnexpectedExceptionToaster.announce(context, e)
			}
		}
	}

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		val halfWidth by remember { derivedStateOf { maxWidth / 2 } }
		val halfWidthPx by LocalDensity.current.run { remember { derivedStateOf { halfWidth.toPx() } } }

		val maxWidthPx by LocalDensity.current.run { remember { derivedStateOf { maxWidth.toPx() } } }

		var previousBrowserDragValue by rememberSaveable {
			mutableStateOf(SlideOutState.Open)
		}
		var browserDragValue by rememberSaveable {
			mutableStateOf(SlideOutState.Open)
		}

		val browserDrawerState = with (LocalDensity.current) {
			remember {
				AnchoredDraggableState(
					initialValue = browserDragValue,
					anchors = DraggableAnchors {
						SlideOutState.Closed at -halfWidthPx
						SlideOutState.Open at 0f
					},
					positionalThreshold = { d -> d * .5f },
					velocityThreshold = { 100.dp.toPx() },
					snapAnimationSpec = tween(),
					decayAnimationSpec = exponentialDecay(),
					confirmValueChange = { newValue ->
						previousBrowserDragValue = browserDragValue
						browserDragValue = newValue
						true
					}
				)
			}
		}

		val browserDrawerOffset by LocalDensity.current.run {
			remember {
				derivedStateOf {
					browserDrawerState.requireOffset().toDp()
				}
			}
		}

		val nowPlayingWidth by remember { derivedStateOf { (halfWidth - browserDrawerOffset).coerceAtMost(maxWidth) } }
		val nowPlayingOffset by remember { derivedStateOf { halfWidth + browserDrawerOffset } }

		var playlistDragValue by rememberSaveable { mutableStateOf(SlideOutState.Closed) }

		val playlistDrawerState = with (LocalDensity.current) {
			remember {
				AnchoredDraggableState(
					initialValue = playlistDragValue,
					anchors = DraggableAnchors {
						SlideOutState.Closed at maxWidthPx
						SlideOutState.Open at halfWidthPx
					},
					positionalThreshold = { d -> d * .5f },
					velocityThreshold = { 100.dp.toPx() },
					snapAnimationSpec = tween(),
					decayAnimationSpec = exponentialDecay(),
					confirmValueChange = { newValue ->
						playlistDragValue = newValue
						true
					}
				)
			}
		}

		suspend fun hidePlaylist() {
			browserViewDependencies.nowPlayingPlaylistViewModel.finishPlaylistEdit()
			playlistDrawerState.animateTo(SlideOutState.Closed)
		}

		val playlistDrawerOffset by LocalDensity.current.run {
			remember {
				derivedStateOf {
					playlistDrawerState
						.requireOffset()
						.toDp()
				}
			}
		}

		val scope = rememberCoroutineScope()

		val isBrowserOpen by remember {
			derivedStateOf {
				browserDrawerState.targetValue == SlideOutState.Open
					|| browserDrawerState.currentValue == SlideOutState.Open
			}
		}

		if (isBrowserOpen) {
			Box(
				modifier = Modifier
					.offset(x = browserDrawerOffset)
					.width(halfWidth)
					.fillMaxHeight()
					.focusGroup()
			) {
				NavigateToTvLibraryDestination(browserViewDependencies)
			}
		} else {
			BackHandler {
				scope.launch {
					when {
						playlistDrawerState.currentValue == SlideOutState.Open -> {
							hidePlaylist()
						}

						browserDrawerState.currentValue == SlideOutState.Closed -> {
							browserDrawerState.animateTo(SlideOutState.Open)
						}
					}
				}
			}
		}

		Box(
			modifier = Modifier
				.width(nowPlayingWidth)
				.offset { IntOffset(x = nowPlayingOffset.roundToPx(), y = 0) }
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

						val isPlaylistOpen by remember {
							derivedStateOf {
								playlistDrawerState.targetValue == SlideOutState.Open
									|| playlistDrawerState.currentValue == SlideOutState.Open
							}
						}

						val nowPlayingControlsWidth by remember {
							derivedStateOf {
								nowPlayingWidth.coerceAtMost(playlistDrawerOffset)
							}
						}

						Box(
							modifier = Modifier
								.fillMaxHeight()
								.width(nowPlayingControlsWidth)
								.focusGroup()
						) {
							NowPlayingHeadline(
								modifier = Modifier.fillMaxWidth(),
								nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel)

							Column(modifier = Modifier
								.align(Alignment.BottomCenter)
								.fillMaxWidth()
							) {
								Row(
									verticalAlignment = Alignment.CenterVertically,
								) {
									Image(
										painter = painterResource(
											if (browserDragValue == SlideOutState.Open) R.drawable.baseline_fullscreen_36
											else R.drawable.baseline_fullscreen_exit_36),
										alpha = playlistControlAlpha,
										contentDescription = stringResource(R.string.btn_hide_files),
										modifier = Modifier
											.navigable(onClick = {
												scope.launch {
													browserDrawerState.animateTo(
														if (browserDragValue == SlideOutState.Open) SlideOutState.Closed
														else SlideOutState.Open
													)
												}
											})
									)

									NowPlayingRating(
										nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
										modifier = Modifier.weight(1f)
									)

									val nowPlayingOpenProgress by remember {
										derivedStateOf {
											with (playlistDrawerState) {
												calculateProgress(
													anchors.positionOf(SlideOutState.Closed),
													anchors.positionOf(SlideOutState.Open),
													requireOffset()
												)
											}
										}
									}

									val browserChevronRotation by remember { derivedStateOf { (-90 + (180 * nowPlayingOpenProgress)).coerceIn(-90f, 180f) } }
									Image(
										painter = painterResource(R.drawable.chevron_up_white_36dp),
										alpha = playlistControlAlpha,
										contentDescription = stringResource(R.string.btn_hide_files),
										modifier = Modifier
											.navigable(onClick = {
												if (playlistDrawerState.currentValue == SlideOutState.Open) {
													scope.launch {
														hidePlaylist()
													}
													scope.launch {
														browserDrawerState.animateTo(previousBrowserDragValue)
													}
												} else {
													scope.launch {
														playlistDrawerState.animateTo(SlideOutState.Open)
													}
													scope.launch {
														browserDrawerState.animateTo(SlideOutState.Closed)
													}
												}
											})
											.rotate(browserChevronRotation),
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

						DisposableEffect(key1 = Unit) {
							if (!isPlaylistOpen)
								nowPlayingPlaylistViewModel.enableSystemAutoScrolling()

							onDispose {  }
						}

						if (isPlaylistOpen) {
							Column(
								modifier = Modifier
									.fillMaxHeight()
									.width(halfWidth)
									.offset(x = playlistDrawerOffset)
									.background(SharedColors.overlayDark)
									.onFocusChanged { state ->
										if (state.hasFocus)
											nowPlayingPlaylistViewModel.disableSystemAutoScrolling()
										else
											nowPlayingPlaylistViewModel.enableSystemAutoScrolling()
									}
									.focusGroup(),
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
									nowPlayingPlaylistViewModel,
									viewModelMessageBus = nowPlayingViewModelMessageBus,
									modifier = Modifier.fillMaxHeight(),
								)
							}
						}
					}
				}
			}
		}
	}

	val connectionWatcherViewModel = browserViewDependencies.connectionWatcherViewModel
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LibraryDestination.Navigate(browserViewDependencies: ScopedViewModelDependencies) {
	when (this) {
		is BrowserLibraryDestination -> {
			NowPlayingTvView(browserViewDependencies = browserViewDependencies)
		}

		is FileDetailsScreen -> {
			val fileDetailsViewModel = browserViewDependencies.fileDetailsViewModel
			FileDetailsView(fileDetailsViewModel, browserViewDependencies.applicationNavigation)

			fileDetailsViewModel.loadFromList(libraryId, playlist, position)
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
						navigateApplication = applicationNavigation,
						stringResources = stringResources,
						userSslCertificates = userSslCertificateProvider,
					)
				}

				viewModel.loadLibrary(libraryId)
			}
		}

		is NowPlayingScreen -> {}
	}
}


@Composable
fun NowPlayingTvPlaylist(
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	modifier: Modifier = Modifier,
) {
	val nowPlayingFiles by playlistViewModel.nowPlayingList.subscribeAsState()
	val playlist by remember { derivedStateOf { nowPlayingFiles.map { p -> p.serviceFile } } }
	val activeLibraryId by nowPlayingFilePropertiesViewModel.activeLibraryId.subscribeAsState()

	val lazyListState = rememberLazyListState()

	val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.subscribeAsState()

	val isAutoScrollEnabled by playlistViewModel.isAutoScrolling.subscribeAsState()
	if (isAutoScrollEnabled) {
		LaunchedEffect(key1 = playingFile) {
			playingFile?.apply {
				if (!lazyListState.isScrollInProgress)
					lazyListState.scrollToItem(playlistPosition)
			}
		}
	}

	val scope = rememberCoroutineScope()
	DisposableEffect(key1 = Unit) {
		val registration = viewModelMessageBus.registerReceiver { _: NowPlayingMessage.ScrollToNowPlaying ->
			scope.launch {
				playingFile?.apply {
					lazyListState.scrollToItem(playlistPosition)
				}
			}
		}

		onDispose {
			registration.close()
		}
	}

	val isEditing by playlistViewModel.isEditingPlaylist.subscribeAsState()
	BackHandler(enabled = isEditing) {
		if (isEditing)
			playlistViewModel.finishPlaylistEdit()
	}

	@Composable
	fun NowPlayingFileView(positionedFile: PositionedFile) {
		val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)

		DisposableEffect(activeLibraryId, positionedFile) {
			activeLibraryId?.also {
				fileItemViewModel.promiseUpdate(it, positionedFile.serviceFile)
			}

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val fileName by fileItemViewModel.title.collectAsState()
		val artist by fileItemViewModel.artist.collectAsState()
		val isPlaying by remember { derivedStateOf { playingFile == positionedFile } }

		NowPlayingTvItemView(
			itemName = fileName,
			artist = artist,
			isActive = isPlaying,
			isEditingPlaylist = isEditing,
			onMoveItemUp = {
				activeLibraryId?.let {
					val currentPosition = positionedFile.playlistPosition
					val newPosition = currentPosition - 1
					playlistViewModel.swapFiles(currentPosition, newPosition)
					playbackServiceController.moveFile(it, currentPosition, newPosition)
				}
			},
			onMoveItemDown = {
				activeLibraryId?.let {
					val currentPosition = positionedFile.playlistPosition
					val newPosition = currentPosition + 1
					playlistViewModel.swapFiles(currentPosition, newPosition)
					playbackServiceController.moveFile(it, currentPosition, newPosition)
				}
			},
			onItemClick = {
				activeLibraryId?.also {
					applicationNavigation.viewFileDetails(
						it,
						playlist,
						positionedFile.playlistPosition
					)
				}
			},
			onRemoveFromNowPlayingClick = {
				activeLibraryId?.also {
					playbackServiceController
						.removeFromPlaylistAtPosition(it, positionedFile.playlistPosition)
				}
			}
		)
	}

	LazyColumn(
		state = lazyListState,
		modifier = Modifier
			.focusGroup()
			.onFocusChanged { f ->
				if (f.hasFocus) playlistViewModel.lockOutAutoScroll()
				else playlistViewModel.releaseAutoScroll()
			}
			.then(modifier),
	) {
		items(items = nowPlayingFiles, key = { f -> f }) { f ->
			NowPlayingFileView(positionedFile = f)
		}
	}
}

private class NowPlayingNavigation(
	inner: NavigateApplication,
	private val navController: NavController<Destination>,
) : NavigateApplication by inner {
	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		navController.navigate(FileDetailsScreen(libraryId, playlist, position))
		return Unit.toPromise()
	}
}

private class NowPlayingDependencies<T>(inner: T, navController: NavController<Destination>)
	: EntryDependencies by inner, AutoCloseable by inner
	where T : EntryDependencies, T: AutoCloseable
{
	override val applicationNavigation by lazy {
		NowPlayingNavigation(inner.applicationNavigation, navController)
	}
}

@Composable
fun NowPlayingTvApplication(
	entryDependencies: EntryDependencies,
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
				entryDependencies.applicationNavigation,
				navController,
				coroutineScope,
				entryDependencies.itemListMenuBackPressedHandler
			)
		)
	}

	val connectionStatusViewModel = viewModel {
		with (RetryingConnectionApplicationDependencies(entryDependencies)) {
			ConnectionStatusViewModel(
				stringResources,
				DramaticConnectionInitializationController(
					connectionSessions,
					destinationRoutingNavigation,
				),
			)
		}
	}

	val routedNavigationDependencies = remember {
		NowPlayingDependencies(
			RoutedNavigationDependencies(
				entryDependencies,
				destinationRoutingNavigation,
				connectionStatusViewModel,
				navController,
				initialDestination
			),
			navController
		)
	}

	val libraryConnectedDependencies = remember {
		RateLimitedFilePropertiesDependencies(
			routedNavigationDependencies,
			RateLimitingExecutionPolicy(1),
		)
	}

	val viewModelStoreOwner = LocalViewModelStoreOwner.current ?: return
	val reusedViewModelDependencies = remember {
		ReusedViewModelRegistry(
			routedNavigationDependencies,
			libraryConnectedDependencies,
			viewModelStoreOwner
		)
	}

	DisposableEffect(key1 = routedNavigationDependencies) {
		onDispose {
			routedNavigationDependencies.close()
		}
	}

	BackHandler { routedNavigationDependencies.applicationNavigation.backOut() }

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
							ScopedViewModelRegistry(reusedViewModelDependencies, permissionsDependencies, it)
						)
					}
				}
				is ApplicationSettingsScreen -> {
					Box(
						modifier = Modifier
							.fillMaxSize()
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
							ScopedViewModelRegistry(reusedViewModelDependencies, permissionsDependencies, it)
						}
						?.apply {
							Box(
								modifier = Modifier.fillMaxSize()
							) {
								TvLibrarySettingsView(
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
				modifier = Modifier.fillMaxSize()
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
