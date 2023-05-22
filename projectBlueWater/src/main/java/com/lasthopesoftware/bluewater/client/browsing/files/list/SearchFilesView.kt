package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberCalculatedKnobHeight
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import java.io.IOException
import kotlin.math.pow

@Composable
fun SearchFilesView(
    searchFilesViewModel: SearchFilesViewModel,
    nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
    itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
    applicationNavigation: NavigateApplication,
    playbackServiceController: ControlPlaybackService,
) {
	val files by searchFilesViewModel.files.collectAsState()
	val playingFile by nowPlayingViewModel.nowPlayingFile.collectAsState()
	var isConnectionLost by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	@Composable
	fun RenderTrackHeaderItem(position: Int, serviceFile: ServiceFile) {
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

		DisposableEffect(serviceFile) {
			searchFilesViewModel.libraryId?.also {
				fileItemViewModel.promiseUpdate(it, serviceFile)
			}

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
		val fileName by fileItemViewModel.title.collectAsState()

		val viewFilesClickHandler = {
			searchFilesViewModel.libraryId?.also {
				applicationNavigation.viewFileDetails(it, files, position)
			}
			Unit
		}

		TrackHeaderItemView(
			itemName = fileName,
			isActive = playingFile?.serviceFile == serviceFile,
			isHiddenMenuShown = isMenuShown,
			onItemClick = viewFilesClickHandler,
			onHiddenMenuClick = {
				itemListMenuBackPressedHandler.hideAllMenus()
				fileItemViewModel.showMenu()
			},
			onAddToNowPlayingClick = {
				playbackServiceController.addToPlaylist(serviceFile)
			},
			onViewFilesClick = viewFilesClickHandler,
			onPlayClick = {
				fileItemViewModel.hideMenu()
				searchFilesViewModel.libraryId?.also {
					playbackServiceController.startPlaylist(it, files, position)
				}
			}
		)
	}

	ControlSurface {
		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by remember { derivedStateOf { 1 - toolbarState.toolbarState.progress } }
		val isLoading by searchFilesViewModel.isLoading.collectAsState()

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val appBarHeight = Dimensions.appBarHeight
				val searchFieldPadding = 16.dp
				val minimumMenuWidth = (2 * 32).dp

				val expandedMenuVerticalPadding = 4.dp
				val boxHeight = appBarHeight + Dimensions.menuHeight + expandedMenuVerticalPadding * 2 + searchFieldPadding * 2

				val acceleratedToolbarStateProgress by remember {
					derivedStateOf {
						toolbarState.toolbarState.progress.pow(
							5
						).coerceIn(0f, 1f)
					}
				}
				val acceleratedHeaderHidingProgress by remember {
					derivedStateOf { 1 - acceleratedToolbarStateProgress }
				}

				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight)
						.fillMaxWidth()
				) {
					if (files.any()) {

						val iconSize = Dimensions.topMenuIconSize
						val menuWidth by remember { derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedHeaderHidingProgress) } }
						val expandedTopRowPadding = appBarHeight + expandedMenuVerticalPadding + searchFieldPadding * 2
						val collapsedTopRowPadding = searchFieldPadding + appBarHeight / 2 - iconSize / 2
						val topRowPadding by remember { derivedStateOf { (expandedTopRowPadding - (expandedTopRowPadding - collapsedTopRowPadding) * headerHidingProgress) } }
						Row(
							modifier = Modifier
								.padding(
									top = topRowPadding,
									bottom = expandedMenuVerticalPadding,
									start = 8.dp,
									end = 8.dp
								)
								.width(menuWidth)
								.align(Alignment.TopEnd)
						) {
							val textModifier = Modifier.alpha(acceleratedToolbarStateProgress)

							val playLabel = stringResource(id = R.string.btn_play)
							ColumnMenuIcon(
								onClick = {
									searchFilesViewModel.libraryId?.also {
										playbackServiceController.startPlaylist(it, files, 0)
									}
							  	},
								icon = {
									Image(
										painter = painterResource(id = R.drawable.av_play),
										contentDescription = playLabel,
										modifier = Modifier.size(iconSize)
									)
								},
								label = if (acceleratedHeaderHidingProgress < 1) playLabel else null,
								labelModifier = textModifier,
								labelMaxLines = 1,
							)

							val shuffleLabel = stringResource(id = R.string.btn_shuffle_files)
							ColumnMenuIcon(
								onClick = {
									searchFilesViewModel.libraryId?.also {
										playbackServiceController.shuffleAndStartPlaylist(it, files)
									}
							  	},
								icon = {
									Image(
										painter = painterResource(id = R.drawable.av_shuffle),
										contentDescription = shuffleLabel,
										modifier = Modifier.size(iconSize)
									)
								},
								label = if (acceleratedHeaderHidingProgress < 1) shuffleLabel else null,
								labelModifier = textModifier,
								labelMaxLines = 1,
							)
						}
					}
				}

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(searchFieldPadding)
						.height(appBarHeight),
					horizontalArrangement = Arrangement.Center,
				) {
					Icon(
						Icons.Default.ArrowBack,
						contentDescription = "",
						tint = MaterialTheme.colors.onSurface,
						modifier = Modifier
							.padding(end = 16.dp)
							.align(Alignment.CenterVertically)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = applicationNavigation::backOut
							)
					)

					val endPadding by remember { derivedStateOf { 4.dp + minimumMenuWidth * acceleratedHeaderHidingProgress } }
					val query by searchFilesViewModel.query.collectAsState()
					val isLibraryIdActive by searchFilesViewModel.isLibraryIdActive.collectAsState()

					TextField(
						value = query,
						placeholder = { stringResource(id = R.string.lbl_search_hint) },
						onValueChange = { searchFilesViewModel.query.value = it },
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
						trailingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.search)) },
						enabled = isLibraryIdActive && !isLoading,
						modifier = Modifier
							.padding(end = endPadding)
							.weight(1f)
					)
				}
			},
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
								Box(
									modifier = Modifier
										.padding(4.dp)
										.height(48.dp)
								) {
									ProvideTextStyle(MaterialTheme.typography.h5) {
										Text(
											text = stringResource(R.string.file_count_label, files.size),
											fontWeight = FontWeight.Bold,
											modifier = Modifier
												.padding(4.dp)
												.align(Alignment.CenterStart)
										)
									}
								}
							}

							itemsIndexed(files) { i, f ->
								RenderTrackHeaderItem(i, f)

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
		}
	}
}
