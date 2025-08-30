package com.lasthopesoftware.bluewater.client.browsing.files.list.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.AnchoredChips
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.LabelledRefreshButton
import com.lasthopesoftware.bluewater.android.ui.components.rememberAnchoredScrollConnectionState
import com.lasthopesoftware.bluewater.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledPlayButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledShuffleButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackTitleItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListContentType
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsMutableState
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import java.io.IOException

private val searchFieldPadding = Dimensions.topRowOuterPadding
private val textFieldHeight = TextFieldDefaults.MinHeight + TextFieldDefaults.FocusedBorderThickness * 2
private val topBarHeight = textFieldHeight + searchFieldPadding

@Composable
fun RowScope.LabelledRefreshButton(
	searchFilesViewModel: SearchFilesViewModel,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null,
) {
	LabelledRefreshButton(
		onClick = {
			searchFilesViewModel.promiseRefresh()
		},
		modifier = modifier,
		focusRequester,
	)
}

@Composable
fun RenderTrackTitleItem(
	position: Int,
	serviceFile: ServiceFile,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	searchFilesViewModel: SearchFilesViewModel,
	applicationNavigation: NavigateApplication,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	playbackServiceController: ControlPlaybackService,
) {
	val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

	DisposableEffect(serviceFile) {
		searchFilesViewModel.loadedLibraryId?.also {
			fileItemViewModel.promiseUpdate(it, serviceFile)
		}

		onDispose {
			fileItemViewModel.reset()
		}
	}

	val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
	val fileName by fileItemViewModel.title.collectAsState()

	val viewFilesClickHandler = {
		searchFilesViewModel.loadedLibraryId?.also {
			applicationNavigation.viewFileDetails(it, searchFilesViewModel.files.value, position)
		}
		Unit
	}

	val playingFile by nowPlayingViewModel.nowPlayingFile.subscribeAsState()
	TrackTitleItemView(
		itemName = fileName,
		isActive = playingFile?.serviceFile == serviceFile,
		isHiddenMenuShown = isMenuShown,
		onItemClick = viewFilesClickHandler,
		onHiddenMenuClick = {
			itemListMenuBackPressedHandler.hideAllMenus()
			fileItemViewModel.showMenu()
		},
		onAddToNowPlayingClick = {
			searchFilesViewModel.loadedLibraryId?.also {
				playbackServiceController.addToPlaylist(it, serviceFile)
			}
		},
		onViewFilesClick = viewFilesClickHandler,
		onPlayClick = {
			fileItemViewModel.hideMenu()
			searchFilesViewModel.loadedLibraryId?.also {
				playbackServiceController.startPlaylist(
					it,
					searchFilesViewModel.files.value,
					position
				)
			}
		}
	)
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun SearchFilesView(
	searchFilesViewModel: SearchFilesViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	stringResources: GetStringResources,
	backStackBuilder: UndoStack
) {
	val files by searchFilesViewModel.files.subscribeAsState()
	var isConnectionLost by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	ControlSurface {
		val isLoading by searchFilesViewModel.isLoading.subscribeAsState()

		Column(
			modifier = Modifier.fillMaxSize()
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.background(MaterialTheme.colors.surface)
			) headerColumn@{
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(start = searchFieldPadding, end = searchFieldPadding)
						.requiredHeight(topBarHeight),
					horizontalArrangement = Arrangement.Start,
					verticalAlignment = Alignment.CenterVertically,
				) {
					BackButton(
						onBack = applicationNavigation::navigateUp,
						modifier = Modifier.padding(end = Dimensions.topRowOuterPadding)
					)

					var query by searchFilesViewModel.query.subscribeAsMutableState()
					val isLibraryIdActive by searchFilesViewModel.isLibraryIdActive.subscribeAsState()
					TextField(
						value = query,
						placeholder = { stringResource(id = R.string.lbl_search_hint) },
						onValueChange = {
							query = it
							if (query.isNotEmpty()) {
								backStackBuilder.addAction {
									searchFilesViewModel.clearResults().toPromise()
								}
							}
						},
						singleLine = true,
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
						keyboardActions = KeyboardActions(onSearch = {
							scope.launch {
								try {
									searchFilesViewModel.findFiles().suspend()
								} catch (e: IOException) {
									isConnectionLost = ConnectionLostExceptionFilter.isConnectionLostException(e)
								}
							}
						}),
						trailingIcon = {
							Icon(
								painter = painterResource(R.drawable.search_24dp),
								contentDescription = stringResource(id = R.string.search),
								modifier = Modifier.clickable {
									scope.launch {
										try {
											searchFilesViewModel.findFiles().suspend()
										} catch (e: IOException) {
											isConnectionLost =
												ConnectionLostExceptionFilter.isConnectionLostException(e)
										}
									}
								}
							)
						},
						enabled = isLibraryIdActive && !isLoading,
						modifier = Modifier.weight(1f, fill = true)
					)
				}
			}

			when {
				isLoading -> {
					Box(modifier = Modifier.fillMaxSize()) {
						CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
					}
				}

				isConnectionLost -> {
					ConnectionLostView(
						onCancel = { isConnectionLost = false },
						onRetry = {
							scope.launch {
								try {
									searchFilesViewModel.findFiles().suspend()
								} catch (e: IOException) {
									isConnectionLost = ConnectionLostExceptionFilter.isConnectionLostException(e)
								}
							}
						}
					)
				}

				files.any() -> {
					BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
						val lazyListState = rememberLazyListState()

						val refreshButtonFocus = remember { FocusRequester() }
						LazyColumn(
							state = lazyListState,
							modifier = Modifier
								.scrollbar(
									lazyListState,
									horizontal = false,
									knobColor = MaterialTheme.colors.onSurface,
									trackColor = Color.Transparent,
									visibleAlpha = .4f,
									knobCornerRadius = 1.dp,
								),
						) {
							item(contentType = ItemListContentType.Menu) {
								if (files.any()) {
									Row(
										modifier = Modifier
											.padding(Dimensions.rowPadding)
											.fillMaxWidth()
									) {
										LabelledPlayButton(
											libraryState = searchFilesViewModel,
											playbackServiceController = playbackServiceController,
											serviceFilesListState = searchFilesViewModel,
										)

										LabelledShuffleButton(
											libraryState = searchFilesViewModel,
											playbackServiceController = playbackServiceController,
											serviceFilesListState = searchFilesViewModel,
										)

										LabelledRefreshButton(
											searchFilesViewModel,
											focusRequester = refreshButtonFocus
										)
									}
								}
							}

							item(contentType = ItemListContentType.Header) {
								Box(
									modifier = Modifier
										.padding(Dimensions.viewPaddingUnit)
										.height(Dimensions.viewPaddingUnit * 12)
								) {
									ProvideTextStyle(MaterialTheme.typography.h5) {
										Text(
											text = stringResource(R.string.file_count_label, files.size),
											fontWeight = FontWeight.Bold,
											modifier = Modifier
												.padding(Dimensions.viewPaddingUnit)
												.align(Alignment.CenterStart)
										)
									}
								}
							}

							itemsIndexed(files, contentType = { _, _ -> ItemListContentType.Item }) { i, f ->
								RenderTrackTitleItem(
									position = i,
									serviceFile = f,
									trackHeadlineViewModelProvider = trackHeadlineViewModelProvider,
									searchFilesViewModel = searchFilesViewModel,
									applicationNavigation = applicationNavigation,
									nowPlayingViewModel = nowPlayingViewModel,
									itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
									playbackServiceController = playbackServiceController,
								)

								if (i < files.lastIndex)
									Divider()
							}
						}

						var minVisibleItemsForScroll by remember { mutableIntStateOf(30) }
						LaunchedEffect(lazyListState, searchFilesViewModel) {
							combine(
								snapshotFlow { lazyListState.layoutInfo },
								searchFilesViewModel.isLoading.mapNotNull().asFlow()
							) { info, s -> Pair(info, s) }
								.filterNot { (_, s) -> s }
								.map { (info, _) -> info.visibleItemsInfo.size }
								.filter { it == 0 }
								.distinctUntilChanged()
								.take(1)
								.collect {
									minVisibleItemsForScroll = (it * 3).coerceAtLeast(30)
								}
						}

						val labeledAnchors by remember {
							derivedStateOf {
								var totalItems = 1

								if (files.any())
									totalItems += 1 + files.size

								if (totalItems <= minVisibleItemsForScroll) emptyList()
								else listOf(
									Pair(stringResources.top,0f),
									Pair(stringResources.end,1f),
								)
							}
						}

						val progressAnchors by remember {
							derivedStateOf { labeledAnchors.map { (_, p) -> p }.toFloatArray() }
						}

						val anchoredScrollConnectionState = rememberAnchoredScrollConnectionState(progressAnchors)

						LaunchedEffect(anchoredScrollConnectionState) {
							snapshotFlow { anchoredScrollConnectionState.selectedProgress }
								.drop(1) // Ignore initial state
								.map {
									val layoutInfo = lazyListState.layoutInfo
									it?.let { progress ->
										val totalItems = layoutInfo.totalItemsCount

										val newIndex = (totalItems - 1) * progress
										newIndex
									}
								}
								.distinctUntilChanged()
								.collect {
									it?.let { fractionalIndex ->
										if (DebugFlag.isDebugCompilation) {
											Log.d("LoadedItemListView", "Selected index: $it")
										}

										val index = fractionalIndex.toInt()

										lazyListState.scrollToItem(index)

										val offsetPercentage = fractionalIndex - index
										if (offsetPercentage != 0f) {
											val offsetPixels = lazyListState
												.layoutInfo
												.visibleItemsInfo
												.firstOrNull()
												?.size
												?.let { s -> s * offsetPercentage }
												?.takeIf { p -> p != 0f }

											if (offsetPixels != null) {
												lazyListState.scrollBy(offsetPixels)
											}
										}
									}
								}
						}

						// 5in in pixels, pixels/Inch
						val density = LocalDensity.current
						val resources = LocalResources.current
						val maxScrollBarHeight = remember(density, resources, maxHeight) {
							with (density) {
								(2.5f * resources.displayMetrics.ydpi).toDp()
							}.coerceAtMost(maxHeight)
						}

						val localHapticFeedback = LocalHapticFeedback.current
						AnchoredChips(
							modifier = Modifier
								.heightIn(200.dp, maxScrollBarHeight)
								.align(Alignment.BottomEnd),
							anchoredScrollConnectionState = anchoredScrollConnectionState,
							lazyListState = lazyListState,
							chipLabel = { _, p -> labeledAnchors.firstOrNull { (_, lp) -> p == lp }?.let { (s, _) -> Text(s) } },
							onScrollProgress = { p -> anchoredScrollConnectionState.selectedProgress = p },
							onSelected = {
								localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
							}
						)
					}
				}

				else -> {
					Spacer(modifier = Modifier.fillMaxSize())
				}
			}
		}
	}
}
