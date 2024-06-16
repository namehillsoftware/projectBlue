package com.namehillsoftware.projectblue.tv.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.TvApplication
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedBrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
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
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.RoutedNavigationDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingControls
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingCoverArtView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingHeadline
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingProgressIndicator
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.PlaylistControls
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.NowPlayingItemView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.controlRowHeight
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.playlistControlAlpha
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.settings.TvLibrarySettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.ui.absoluteProgressState
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
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.tv.settings.TvApplicationSettingsView
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch
import kotlin.math.abs

private val logger by lazyLogger<TvApplication>()

enum class NowPlayingDragValue { Browser, NowPlaying, NowPlayingList }

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
		val halfWidth by remember { derivedStateOf { maxWidth / 2 } }
		val halfWidthPx by LocalDensity.current.run { remember { derivedStateOf { halfWidth.toPx() } } }

		val maxWidthPx by LocalDensity.current.run { remember { derivedStateOf { maxWidth.toPx() } } }

		val draggableState = with (LocalDensity.current) {
			remember {
				AnchoredDraggableState(
					initialValue = NowPlayingDragValue.Browser,
					anchors = DraggableAnchors {
						NowPlayingDragValue.Browser at 0f
						NowPlayingDragValue.NowPlaying at halfWidthPx
						NowPlayingDragValue.NowPlayingList at maxWidthPx
					},
					positionalThreshold = { d -> d * .5f },
					velocityThreshold = { 100.dp.toPx() },
					animationSpec = tween()
				)
			}
		}

		val dragProgress by draggableState.absoluteProgressState
		val dragOffset by LocalDensity.current.run {
			remember {
				derivedStateOf {
					draggableState.requireOffset().toDp()
				}
			}
		}
		val browserOffset by LocalDensity.current.run {
			remember {
				derivedStateOf {
					-dragOffset
				}
			}
		}
		val nowPlayingWidth by remember { derivedStateOf { (halfWidth + dragOffset).coerceAtMost(maxWidth) } }
		val nowPlayingOffset by remember { derivedStateOf { maxWidth - nowPlayingWidth } }
		val playlistOffset by remember { derivedStateOf { maxWidth + halfWidth - dragOffset } }

		val scope = rememberCoroutineScope()

		val isBrowserShown by remember {
			derivedStateOf {
				draggableState.targetValue == NowPlayingDragValue.Browser
					|| draggableState.currentValue == NowPlayingDragValue.Browser
			}
		}
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
					draggableState.animateTo(
						when (draggableState.currentValue) {
							NowPlayingDragValue.NowPlayingList -> {
								browserViewDependencies.nowPlayingPlaylistViewModel.finishPlaylistEdit()
								NowPlayingDragValue.NowPlaying
							}
							else -> NowPlayingDragValue.Browser
						}
					)
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

						val isPlaylistShown by remember { derivedStateOf { playlistOffset < this@BoxWithConstraints.maxWidth } }
						val nowPlayingOverlayWidth by with (this@BoxWithConstraints) {
							remember {
								derivedStateOf {
									maxWidth - abs(playlistOffset.value - maxWidth.value).dp.coerceAtMost(maxWidth)
								}
							}
						}

						Box(
							modifier = Modifier
								.fillMaxHeight()
								.width(nowPlayingOverlayWidth)
								.focusGroup()
						) {
							NowPlayingHeadline(
								modifier = Modifier.fillMaxWidth(),
								nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel)

							var isMovingRight by remember { mutableStateOf(true) }
							val browserChevronRotation by remember { derivedStateOf { 90 - (180 * dragProgress) } }
							Image(
								painter = painterResource(R.drawable.chevron_up_white_36dp),
								alpha = playlistControlAlpha,
								contentDescription = stringResource(R.string.btn_hide_files),
								modifier = Modifier
									.navigable(onClick = {
										scope.launch {
											draggableState.animateTo(
												when (draggableState.currentValue) {
													NowPlayingDragValue.Browser -> {
														isMovingRight = true
														NowPlayingDragValue.NowPlaying
													}

													NowPlayingDragValue.NowPlaying -> {
														if (isMovingRight) NowPlayingDragValue.NowPlayingList
														else NowPlayingDragValue.Browser
													}

													NowPlayingDragValue.NowPlayingList -> {
														isMovingRight = false
														nowPlayingPlaylistViewModel.finishPlaylistEdit()
														NowPlayingDragValue.NowPlaying
													}
												}
											)
										}
									})
									.rotate(browserChevronRotation)
									.align(Alignment.Center),
							)

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
									.offset(x = playlistOffset)
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
	: BrowserViewDependencies by inner, AutoCloseable by inner
	where T : BrowserViewDependencies, T: AutoCloseable
{
	override val applicationNavigation by lazy {
		NowPlayingNavigation(inner.applicationNavigation, navController)
	}
}


@Composable
fun NowPlayingTvPlaylist(
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	playlistViewModel: NowPlayingPlaylistViewModel,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	modifier: Modifier = Modifier,
) {
	val nowPlayingFiles by playlistViewModel.nowPlayingList.subscribeAsState()
	val playlist by remember { derivedStateOf { nowPlayingFiles.map { p -> p.serviceFile } } }
	val activeLibraryId by nowPlayingFilePropertiesViewModel.activeLibraryId.subscribeAsState()

	val lazyListState = rememberTvLazyListState()

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

		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
		val fileName by fileItemViewModel.title.collectAsState()
		val artist by fileItemViewModel.artist.collectAsState()
		val isPlaying by remember { derivedStateOf { playingFile == positionedFile } }

		val viewFilesClickHandler = {
			activeLibraryId?.also {
				applicationNavigation.viewFileDetails(
					it,
					playlist,
					positionedFile.playlistPosition
				)
			}
			Unit
		}

		NowPlayingItemView(
			itemName = fileName,
			artist = artist,
			isActive = isPlaying,
			isHiddenMenuShown = isMenuShown,
			onItemClick = viewFilesClickHandler,
			onHiddenMenuClick = {
				itemListMenuBackPressedHandler.hideAllMenus()
				fileItemViewModel.showMenu()
			},
			onRemoveFromNowPlayingClick = {
				activeLibraryId?.also {
					playbackServiceController
						.removeFromPlaylistAtPosition(it, positionedFile.playlistPosition)
				}
			},
			onViewFilesClick = viewFilesClickHandler,
			onPlayClick = {
				fileItemViewModel.hideMenu()
				activeLibraryId?.also {
					playbackServiceController.seekTo(it, positionedFile.playlistPosition)
				}
			}
		)
	}

	TvLazyColumn(
		state = lazyListState,
		modifier = Modifier.focusGroup().onFocusChanged { f ->
			if (f.hasFocus) playlistViewModel.lockOutAutoScroll()
			else playlistViewModel.releaseAutoScroll()
		}.then(modifier),
	) {
		items(items = nowPlayingFiles, key = { f -> f }) { f ->
			NowPlayingFileView(positionedFile = f)
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
		NowPlayingDependencies(
			RoutedNavigationDependencies(
				browserViewDependencies,
				destinationRoutingNavigation,
				connectionStatusViewModel,
				navController,
				initialDestination
			),
			navController
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
							ScopedViewModelDependencies(routedNavigationDependencies, permissionsDependencies, it)
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
							ScopedViewModelDependencies(routedNavigationDependencies, permissionsDependencies, it)
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

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
		if (isCheckingConnection) {
			Box(
				modifier = Modifier.fillMaxSize()
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
