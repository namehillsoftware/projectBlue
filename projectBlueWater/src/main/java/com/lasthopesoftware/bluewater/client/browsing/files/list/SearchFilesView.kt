package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabelledRefreshButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.UnlabelledRefreshButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberCalculatedKnobHeight
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsMutableState
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.pow

private val searchFieldPadding = Dimensions.topRowOuterPadding
private val textFieldHeight = TextFieldDefaults.MinHeight + TextFieldDefaults.FocusedBorderThickness * 2
private val topBarHeight = textFieldHeight + searchFieldPadding
private val minimumMenuWidth = (3 * 32).dp

private val expandedMenuVerticalPadding = searchFieldPadding * 2
private val boxHeight = topBarHeight + Dimensions.menuHeight + expandedMenuVerticalPadding

@Composable
fun RowScope.LabelledRefreshButton(
	searchFilesViewModel: SearchFilesViewModel,
	modifier: Modifier = Modifier,
) {
	LabelledRefreshButton(
		onClick = {
			searchFilesViewModel.promiseRefresh()
		},
		modifier = modifier,
	)
}

@Composable
fun RowScope.UnlabelledRefreshButton(
	searchFilesViewModel: SearchFilesViewModel,
) {
	UnlabelledRefreshButton {
		searchFilesViewModel.promiseRefresh()
	}
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
				playbackServiceController.startPlaylist(it, searchFilesViewModel.files.value, position)
			}
		}
	)
}

@Composable
fun SearchFilesView(
	searchFilesViewModel: SearchFilesViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
) {
	val files by searchFilesViewModel.files.subscribeAsState()
	var isConnectionLost by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	ControlSurface {
		val isLoading by searchFilesViewModel.isLoading.subscribeAsState()

		val heightScaler = LocalDensity.current.run {
			memorableScrollConnectedScaler(max = boxHeight.toPx(), min = topBarHeight.toPx())
		}

		Box(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(heightScaler)
		) {
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
						val rowHeight = Dimensions.standardRowHeight
						val lazyListState = rememberLazyListState()
						val knobHeight by rememberCalculatedKnobHeight(lazyListState, rowHeight)
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
									fixedKnobRatio = knobHeight,
								),
						) {
							item {
								Spacer(modifier = Modifier
									.requiredHeight(boxHeight)
									.fillMaxWidth())
							}

							item {
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

							itemsIndexed(files) { i, f ->
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
					}
				}
				else -> {
					Spacer(modifier = Modifier.fillMaxSize())
				}
			}

			val heightValue by heightScaler.getValueState()
			val headerCollapsingProgress by heightScaler.getProgressState()
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(MaterialTheme.colors.surface)
					.padding(start = searchFieldPadding, end = searchFieldPadding)
					.height(LocalDensity.current.run { heightValue.toDp() })
			) {
				val acceleratedHeaderCollapsingProgress by remember {
					derivedStateOf {
						headerCollapsingProgress.pow(
							.2f
						).coerceIn(0f, 1f)
					}
				}

				val acceleratedToolbarExpandingProgress by remember {
					derivedStateOf { 1 - acceleratedHeaderCollapsingProgress }
				}

				if (files.any()) {
					BoxWithConstraints(
						modifier = Modifier.fillMaxWidth()
					) {
						val iconSize = Dimensions.topMenuIconSize
						val menuWidth by remember { derivedStateOf { linearInterpolation(maxWidth, minimumMenuWidth, acceleratedHeaderCollapsingProgress) } }
						val expandedTopRowPadding = topBarHeight + expandedMenuVerticalPadding
						val collapsedTopRowPadding = topBarHeight / 2 - iconSize / 2
						val topRowPadding by remember { derivedStateOf { linearInterpolation(expandedTopRowPadding, collapsedTopRowPadding, headerCollapsingProgress) } }
						Row(
							modifier = Modifier
								.padding(
									top = topRowPadding
								)
								.width(menuWidth)
								.align(Alignment.TopEnd)
						) {
							val textModifier = Modifier.alpha(acceleratedToolbarExpandingProgress)

							if (acceleratedHeaderCollapsingProgress < 1) {
								LabelledPlayButton(
									libraryState = searchFilesViewModel,
									playbackServiceController = playbackServiceController,
									serviceFilesListState = searchFilesViewModel,
									modifier = textModifier
								)

								LabelledShuffleButton(
									libraryState = searchFilesViewModel,
									playbackServiceController = playbackServiceController,
									serviceFilesListState = searchFilesViewModel,
									modifier = textModifier,
								)

								LabelledRefreshButton(
									searchFilesViewModel,
									modifier = textModifier,
								)
							} else {
								UnlabelledPlayButton(
									libraryState = searchFilesViewModel,
									playbackServiceController = playbackServiceController,
									serviceFilesListState = searchFilesViewModel
								)

								UnlabelledShuffleButton(
									libraryState = searchFilesViewModel,
									playbackServiceController = playbackServiceController,
									serviceFilesListState = searchFilesViewModel,
								)

								UnlabelledRefreshButton(searchFilesViewModel)
							}
						}
					}
				}

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.requiredHeight(topBarHeight)
						.align(Alignment.TopStart),
					horizontalArrangement = Arrangement.Start,
					verticalAlignment = Alignment.CenterVertically,
				) {
					BackButton(
						onBack = applicationNavigation::backOut,
						modifier = Modifier.padding(end = Dimensions.topRowOuterPadding)
					)

					val endPadding by remember { derivedStateOf { Dimensions.viewPaddingUnit * 4 + (minimumMenuWidth + 12.dp) * acceleratedHeaderCollapsingProgress } }

					var query by searchFilesViewModel.query.subscribeAsMutableState()
					BackHandler(enabled = query.isNotEmpty()) {
						searchFilesViewModel.clearResults()
					}

					val isLibraryIdActive by searchFilesViewModel.isLibraryIdActive.subscribeAsState()
					TextField(
						value = query,
						placeholder = { stringResource(id = R.string.lbl_search_hint) },
						onValueChange = { query = it },
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
											isConnectionLost = ConnectionLostExceptionFilter.isConnectionLostException(e)
										}
									}
								}
							)
					  	},
						enabled = isLibraryIdActive && !isLoading,
						modifier = Modifier
							.padding(end = endPadding)
							.weight(1f, fill = true)
					)
				}
			}
		}
	}
}
