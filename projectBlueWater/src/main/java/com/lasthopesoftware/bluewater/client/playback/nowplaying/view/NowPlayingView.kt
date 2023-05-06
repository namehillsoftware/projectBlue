package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionLostViewModel
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.NowPlayingItemView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.PlayPauseButton
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.DragDropItemScope
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.DragDropLazyColumn
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.rememberDragDropListState
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import kotlinx.coroutines.launch

@Composable
private fun NowPlayingCoverArtView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
) {
	Box(
		modifier = Modifier.fillMaxSize()
	) {
		val defaultImage by nowPlayingCoverArtViewModel.defaultImage.collectAsState()
		defaultImage
			?.let {
				val defaultImageBitmap by remember { derivedStateOf { it.asImageBitmap() } }
				Image(
					bitmap = defaultImageBitmap,
					contentDescription = stringResource(id = R.string.img_now_playing_loading),
					contentScale = ContentScale.Crop,
					alignment = Alignment.Center,
					modifier = Modifier.fillMaxSize(),
				)
			}

		val isLoadingImage by nowPlayingCoverArtViewModel.isNowPlayingImageLoading.collectAsState()
		if (isLoadingImage) {
			CircularProgressIndicator(
				modifier = Modifier.align(Alignment.Center)
			)
		} else {
			val coverArt by nowPlayingCoverArtViewModel.nowPlayingImage.collectAsState()
			val coverArtBitmap by remember { derivedStateOf { coverArt?.asImageBitmap() } }
			coverArtBitmap
				?.let {
					Image(
						bitmap = it,
						contentDescription = stringResource(id = R.string.img_now_playing),
						contentScale = ContentScale.Crop,
						alignment = Alignment.Center,
						modifier = Modifier.fillMaxSize(),
					)
				}
		}
	}
}

@Composable
private fun KeepScreenOn(keepScreenOn: Boolean) {
	val currentView = LocalView.current
	DisposableEffect(keepScreenOn) {
		currentView.keepScreenOn = keepScreenOn
		onDispose {
			currentView.keepScreenOn = false
		}
	}
}

@Composable
private fun NowPlayingProgressIndicator(fileProgress: Float) {
	LinearProgressIndicator(
		progress = fileProgress,
		color = Color.White,
		backgroundColor = Color.White.copy(alpha = .2f),
		modifier = Modifier.fillMaxWidth()
	)
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NowPlayingView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	screenOnState: NowPlayingScreenViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	connectionLostViewModel: ConnectionLostViewModel
) {
	val isScreenOn by screenOnState.isScreenOn.collectAsState()
	KeepScreenOn(isScreenOn)

	Surface(
		color = Color.Transparent,
		contentColor = Color.White,
	) {
		NowPlayingCoverArtView(nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel)

		val pagerState = rememberPagerState()

		val scope = rememberCoroutineScope()
		BackHandler(pagerState.settledPage > 0) {
			when {
				itemListMenuBackPressedHandler.hideAllMenus() -> {}
				playlistViewModel.isEditingPlaylist -> playlistViewModel.finishPlaylistEdit()
				pagerState.settledPage == 1 -> {
					playlistViewModel.finishPlaylistEdit()
					scope.launch { pagerState.animateScrollToPage(0) }
				}
			}
		}

		val isEditingPlaylist by playlistViewModel.isEditingPlaylistState.collectAsState()
		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

		val lazyListState = rememberLazyListState()
		val snappingLayout = remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) { _, _ -> 0f } }

		val filePosition by nowPlayingFilePropertiesViewModel.filePosition.collectAsState()
		val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.collectAsState()
		val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }

		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.background(SharedColors.OverlayDark)
				.padding(systemBarsPadding),
		) {
			LazyColumn(
				flingBehavior = rememberSnapFlingBehavior(snappingLayout),
				userScrollEnabled = !isEditingPlaylist,
			) {
				item {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.height(maxHeight)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = { nowPlayingFilePropertiesViewModel.showNowPlayingControls() }
							),
					) {
						val isScreenControlsVisible by nowPlayingFilePropertiesViewModel.isScreenControlsVisible.collectAsState()

						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically
						) {
							Column(
								modifier = Modifier.weight(1f)
							) {
								ProvideTextStyle(value = MaterialTheme.typography.h5) {
									val title by nowPlayingFilePropertiesViewModel.title.collectAsState()

									MarqueeText(
										text = title,
										gradientEdgeColor = Color.Transparent,
									)
								}

								ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
									val artist by nowPlayingFilePropertiesViewModel.artist.collectAsState()
									MarqueeText(
										text = artist,
										gradientEdgeColor = Color.Transparent,
									)
								}
							}

							if (isScreenControlsVisible) {
								Row(modifier = Modifier.wrapContentWidth()) {
									val isScreenOnEnabled by screenOnState.isScreenOnEnabled.collectAsState()
									Image(
										painter = painterResource(if (isScreenOnEnabled) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
										alpha = .8f,
										contentDescription = stringResource(if (isScreenOnEnabled) R.string.screen_is_on else R.string.screen_is_off),
										modifier = Modifier
											.padding(Dimensions.viewPaddingUnit)
											.clickable(onClick = screenOnState::toggleScreenOn),
									)

									Image(
										painter = painterResource(R.drawable.chevron_up_white_36dp),
										alpha = .8f,
										contentDescription = stringResource(R.string.btn_view_files),
										modifier = Modifier
											.padding(Dimensions.viewPaddingUnit)
											.clickable(onClick = {
												scope.launch {
													pagerState.animateScrollToPage(1)
												}
											}),
									)
								}
							}
						}

						Column(
							modifier = Modifier
								.align(Alignment.BottomCenter)
								.fillMaxWidth(),
							horizontalAlignment = Alignment.CenterHorizontally,
						) {
							if (isScreenControlsVisible) {
								Box(
									modifier = Modifier
										.fillMaxWidth()
										.padding(Dimensions.viewPaddingUnit)
								) {
									val rating by nowPlayingFilePropertiesViewModel.songRating.collectAsState()
									val ratingInt by remember { derivedStateOf { rating.toInt() } }
									RatingBar(
										rating = ratingInt,
										color = Color.White,
										backgroundColor = Color.White.copy(alpha = .1f),
										modifier = Modifier
											.fillMaxWidth()
											.height(Dimensions.menuHeight),
										onRatingSelected = { nowPlayingFilePropertiesViewModel.updateRating(it.toFloat()) }
									)

									val isReadOnly by nowPlayingFilePropertiesViewModel.isReadOnly.collectAsState()
									if (isReadOnly) {
										ProvideTextStyle(value = MaterialTheme.typography.caption) {
											Text(
												text = stringResource(id = R.string.readOnlyConnection)
											)
										}
									}
								}
							}

							NowPlayingProgressIndicator(fileProgress = fileProgress)

							Row(
								modifier = Modifier
									.fillMaxWidth()
									.height(64.dp),
								horizontalArrangement = Arrangement.SpaceEvenly,
								verticalAlignment = Alignment.CenterVertically,
							) {
								if (isScreenControlsVisible) {
									Image(
										painter = painterResource(id = R.drawable.av_previous_white),
										contentDescription = stringResource(id = R.string.btn_previous),
										modifier = Modifier.clickable {
											playbackServiceController.previous()
										}
									)

									PlayPauseButton(nowPlayingFilePropertiesViewModel, playbackServiceController)

									Image(
										painter = painterResource(id = R.drawable.av_next_white),
										contentDescription = stringResource(id = R.string.btn_next),
										modifier = Modifier.clickable {
											playbackServiceController.next()
										}
									)
								}
							}
						}
					}
				}

				item {
					Box(modifier = Modifier.height(maxHeight)) {
						Column(
							modifier = Modifier.fillMaxSize()
						) {
							Row(
								modifier = Modifier
									.height(56.dp)
									.fillMaxWidth(),
								horizontalArrangement = Arrangement.SpaceAround,
								verticalAlignment = Alignment.CenterVertically,
							) {
								if (isEditingPlaylist) {
									Image(
										painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
										contentDescription = stringResource(id = R.string.finish_edit_now_playing_list),
										modifier = Modifier.clickable {
											playlistViewModel.finishPlaylistEdit()
										},
										alpha = .8f,
									)
								} else {
									Image(
										painter = painterResource(id = R.drawable.pencil),
										contentDescription = stringResource(id = R.string.edit_now_playing_list),
										modifier = Modifier.clickable {
											playlistViewModel.editPlaylist()
										},
										alpha = .8f,
									)
								}

								val isRepeating by nowPlayingFilePropertiesViewModel.isRepeating.collectAsState()
								if (isRepeating) {
									Image(
										painter = painterResource(id = R.drawable.av_repeat_white),
										contentDescription = stringResource(id = R.string.btn_complete_playlist),
										modifier = Modifier.clickable {
											nowPlayingFilePropertiesViewModel.toggleRepeating()
										},
										alpha = .8f,
									)
								} else {
									Image(
										painter = painterResource(id = R.drawable.av_no_repeat_white),
										contentDescription = stringResource(id = R.string.btn_repeat_playlist),
										modifier = Modifier.clickable {
											nowPlayingFilePropertiesViewModel.toggleRepeating()
										},
										alpha = .8f,
									)
								}

								PlayPauseButton(
									nowPlayingFilePropertiesViewModel,
									playbackServiceController,
									alpha = .8f
								)

								Image(
									painter = painterResource(R.drawable.chevron_up_white_36dp),
									alpha = .8f,
									contentDescription = stringResource(R.string.btn_hide_files),
									modifier = Modifier
										.clickable(onClick = {
											playlistViewModel.finishPlaylistEdit()
											scope.launch {
												pagerState.animateScrollToPage(0)
											}
										})
										.rotate(180f),
								)
							}

							NowPlayingProgressIndicator(fileProgress = fileProgress)

							val nowPlayingFiles by playlistViewModel.nowPlayingList.collectAsState()
							val playlist by remember { derivedStateOf { nowPlayingFiles.map { p -> p.serviceFile } } }
							val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.collectAsState()

							val reorderableState = rememberDragDropListState(
								onMove = { from, to ->
									playlistViewModel.swapFiles(from, to)
								},
								onDragEnd = { from, to ->
									playbackServiceController.moveFile(from, to)
								}
							)

							@Composable
							fun DragDropItemScope.NowPlayingFileView(positionedFile: PositionedFile) {
								val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)

								DisposableEffect(positionedFile) {
									nowPlayingFilePropertiesViewModel.activeLibraryId?.also {
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
									nowPlayingFilePropertiesViewModel.activeLibraryId?.also {
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
									isEditingPlaylist = isEditingPlaylist,
									isHiddenMenuShown = isMenuShown,
									onItemClick = viewFilesClickHandler,
									dragDropListState = reorderableState,
									onHiddenMenuClick = {
										if (!isEditingPlaylist) {
											itemListMenuBackPressedHandler.hideAllMenus()
											fileItemViewModel.showMenu()
										}
									},
									onRemoveFromNowPlayingClick = {
										playbackServiceController.removeFromPlaylistAtPosition(positionedFile.playlistPosition)
									},
									onViewFilesClick = viewFilesClickHandler,
									onPlayClick = {
										fileItemViewModel.hideMenu()
										playbackServiceController.seekTo(positionedFile.playlistPosition)
									}
								)
							}

							if (pagerState.currentPage == 0) {
								playingFile?.apply {
									scope.launch {
										reorderableState.lazyListState.scrollToItem(playlistPosition)
									}
								}
							}

//						AndroidView(
//							factory = { context ->
//								RecyclerView(context).apply {
//									clipToOutline = true
//
//									layoutParams = RecyclerView.LayoutParams(
//										RecyclerView.LayoutParams.MATCH_PARENT,
//										RecyclerView.LayoutParams.MATCH_PARENT
//									)
//
//									layoutManager = LinearLayoutManager(context)
//
//									isNestedScrollingEnabled = true
//
//									adapter = ComposeRecyclerViewAdapter<PositionedFile>(context) { f ->
//										NowPlayingFileView(positionedFile = f)
//									}
//								}
//							},
//							modifier = Modifier
//								.weight(1f)
//								.background(SharedColors.OverlayDark),
//							{ recyclerView ->
//								recyclerView.adapter
//									?.let { it as? ComposeRecyclerViewAdapter<PositionedFile> }
//									?.updateListEventually(nowPlayingFiles)
//							}
//						)

							DragDropLazyColumn(
								dragDropListState = reorderableState,
								modifier = Modifier.background(SharedColors.OverlayDark),
							) {
								dragDropItems(items = nowPlayingFiles, keyFactory = { _, f -> f }) { _, f ->
									NowPlayingFileView(positionedFile = f)
								}
							}
						}
					}
				}
			}
		}

//		VerticalPager(
//			pageSize = PageSize.Fill,
//			pageCount = 2,
//			state = pagerState,
//			modifier = Modifier
//				.fillMaxSize()
//				.background(SharedColors.OverlayDark)
//				.padding(systemBarsPadding),
//			userScrollEnabled = !isEditingPlaylist,
//			beyondBoundsPageCount = 1
//		) { page ->
//			val filePosition by nowPlayingFilePropertiesViewModel.filePosition.collectAsState()
//			val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.collectAsState()
//			val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }
//
//			if (page == 0) {
//				Box(
//					modifier = Modifier
//						.fillMaxSize()
//						.clickable(
//							interactionSource = remember { MutableInteractionSource() },
//							indication = null,
//							onClick = { nowPlayingFilePropertiesViewModel.showNowPlayingControls() }
//						),
//				) {
//					val isScreenControlsVisible by nowPlayingFilePropertiesViewModel.isScreenControlsVisible.collectAsState()
//
//					Row(
//						modifier = Modifier.fillMaxWidth(),
//						horizontalArrangement = Arrangement.SpaceBetween,
//						verticalAlignment = Alignment.CenterVertically
//					) {
//						Column(
//							modifier = Modifier.weight(1f)
//						) {
//							ProvideTextStyle(value = MaterialTheme.typography.h5) {
//								val title by nowPlayingFilePropertiesViewModel.title.collectAsState()
//
//								MarqueeText(
//									text = title,
//									gradientEdgeColor = Color.Transparent,
//								)
//							}
//
//							ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
//								val artist by nowPlayingFilePropertiesViewModel.artist.collectAsState()
//								MarqueeText(
//									text = artist,
//									gradientEdgeColor = Color.Transparent,
//								)
//							}
//						}
//
//						if (isScreenControlsVisible) {
//							Row(modifier = Modifier.wrapContentWidth()) {
//								val isScreenOnEnabled by screenOnState.isScreenOnEnabled.collectAsState()
//								Image(
//									painter = painterResource(if (isScreenOnEnabled) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
//									alpha = .8f,
//									contentDescription = stringResource(if (isScreenOnEnabled) R.string.screen_is_on else R.string.screen_is_off),
//									modifier = Modifier
//										.padding(Dimensions.viewPaddingUnit)
//										.clickable(onClick = screenOnState::toggleScreenOn),
//								)
//
//								Image(
//									painter = painterResource(R.drawable.chevron_up_white_36dp),
//									alpha = .8f,
//									contentDescription = stringResource(R.string.btn_view_files),
//									modifier = Modifier
//										.padding(Dimensions.viewPaddingUnit)
//										.clickable(onClick = {
//											scope.launch {
//												pagerState.animateScrollToPage(1)
//											}
//										}),
//								)
//							}
//						}
//					}
//
//					Column(
//						modifier = Modifier
//							.align(Alignment.BottomCenter)
//							.fillMaxWidth(),
//						horizontalAlignment = Alignment.CenterHorizontally,
//					) {
//						if (isScreenControlsVisible) {
//							Box(
//								modifier = Modifier
//									.fillMaxWidth()
//									.padding(Dimensions.viewPaddingUnit)
//							) {
//								val rating by nowPlayingFilePropertiesViewModel.songRating.collectAsState()
//								val ratingInt by remember { derivedStateOf { rating.toInt() } }
//								RatingBar(
//									rating = ratingInt,
//									color = Color.White,
//									backgroundColor = Color.White.copy(alpha = .1f),
//									modifier = Modifier
//										.fillMaxWidth()
//										.height(Dimensions.menuHeight),
//									onRatingSelected = { nowPlayingFilePropertiesViewModel.updateRating(it.toFloat()) }
//								)
//
//								val isReadOnly by nowPlayingFilePropertiesViewModel.isReadOnly.collectAsState()
//								if (isReadOnly) {
//									ProvideTextStyle(value = MaterialTheme.typography.caption) {
//										Text(
//											text = stringResource(id = R.string.readOnlyConnection)
//										)
//									}
//								}
//							}
//						}
//
//						NowPlayingProgressIndicator(fileProgress = fileProgress)
//
//						Row(
//							modifier = Modifier
//								.fillMaxWidth()
//								.height(64.dp),
//							horizontalArrangement = Arrangement.SpaceEvenly,
//							verticalAlignment = Alignment.CenterVertically,
//						) {
//							if (isScreenControlsVisible) {
//								Image(
//									painter = painterResource(id = R.drawable.av_previous_white),
//									contentDescription = stringResource(id = R.string.btn_previous),
//									modifier = Modifier.clickable {
//										playbackServiceController.previous()
//									}
//								)
//
//								PlayPauseButton(nowPlayingFilePropertiesViewModel, playbackServiceController)
//
//								Image(
//									painter = painterResource(id = R.drawable.av_next_white),
//									contentDescription = stringResource(id = R.string.btn_next),
//									modifier = Modifier.clickable {
//										playbackServiceController.next()
//									}
//								)
//							}
//						}
//					}
//				}
//			} else {
//				Box {
//					Column(
//						modifier = Modifier.fillMaxSize()
//					) {
//						Row(
//							modifier = Modifier
//								.height(56.dp)
//								.fillMaxWidth(),
//							horizontalArrangement = Arrangement.SpaceAround,
//							verticalAlignment = Alignment.CenterVertically,
//						) {
//							if (isEditingPlaylist) {
//								Image(
//									painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
//									contentDescription = stringResource(id = R.string.finish_edit_now_playing_list),
//									modifier = Modifier.clickable {
//										playlistViewModel.finishPlaylistEdit()
//									},
//									alpha = .8f,
//								)
//							} else {
//								Image(
//									painter = painterResource(id = R.drawable.pencil),
//									contentDescription = stringResource(id = R.string.edit_now_playing_list),
//									modifier = Modifier.clickable {
//										playlistViewModel.editPlaylist()
//									},
//									alpha = .8f,
//								)
//							}
//
//							val isRepeating by nowPlayingFilePropertiesViewModel.isRepeating.collectAsState()
//							if (isRepeating) {
//								Image(
//									painter = painterResource(id = R.drawable.av_repeat_white),
//									contentDescription = stringResource(id = R.string.btn_complete_playlist),
//									modifier = Modifier.clickable {
//										nowPlayingFilePropertiesViewModel.toggleRepeating()
//									},
//									alpha = .8f,
//								)
//							} else {
//								Image(
//									painter = painterResource(id = R.drawable.av_no_repeat_white),
//									contentDescription = stringResource(id = R.string.btn_repeat_playlist),
//									modifier = Modifier.clickable {
//										nowPlayingFilePropertiesViewModel.toggleRepeating()
//									},
//									alpha = .8f,
//								)
//							}
//
//							PlayPauseButton(
//								nowPlayingFilePropertiesViewModel,
//								playbackServiceController,
//								alpha = .8f
//							)
//
//							Image(
//								painter = painterResource(R.drawable.chevron_up_white_36dp),
//								alpha = .8f,
//								contentDescription = stringResource(R.string.btn_hide_files),
//								modifier = Modifier
//									.clickable(onClick = {
//										playlistViewModel.finishPlaylistEdit()
//										scope.launch {
//											pagerState.animateScrollToPage(0)
//										}
//									})
//									.rotate(180f),
//							)
//						}
//
//						NowPlayingProgressIndicator(fileProgress = fileProgress)
//
//						val nowPlayingFiles by playlistViewModel.nowPlayingList.collectAsState()
//						val playlist by remember { derivedStateOf { nowPlayingFiles.map { p -> p.serviceFile } } }
//						val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.collectAsState()
//
//						val reorderableState = rememberDragDropListState(
//							onMove = { from, to ->
//								playlistViewModel.swapFiles(from, to)
//							},
//							onDragEnd = { from, to ->
//								playbackServiceController.moveFile(from, to)
//							}
//						)
//
//						@Composable
//						fun DragDropItemScope.NowPlayingFileView(positionedFile: PositionedFile) {
//							val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)
//
//							DisposableEffect(positionedFile) {
//								nowPlayingFilePropertiesViewModel.activeLibraryId?.also {
//									fileItemViewModel.promiseUpdate(it, positionedFile.serviceFile)
//								}
//
//								onDispose {
//									fileItemViewModel.reset()
//								}
//							}
//
//							val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
//							val fileName by fileItemViewModel.title.collectAsState()
//							val artist by fileItemViewModel.artist.collectAsState()
//							val isPlaying by remember { derivedStateOf { playingFile == positionedFile } }
//
//							val viewFilesClickHandler = {
//								nowPlayingFilePropertiesViewModel.activeLibraryId?.also {
//									applicationNavigation.viewFileDetails(
//										it,
//										playlist,
//										positionedFile.playlistPosition
//									)
//								}
//								Unit
//							}
//
//							NowPlayingItemView(
//								itemName = fileName,
//								artist = artist,
//								isActive = isPlaying,
//								isEditingPlaylist = isEditingPlaylist,
//								isHiddenMenuShown = isMenuShown,
//								onItemClick = viewFilesClickHandler,
//								dragDropListState = reorderableState,
//								onHiddenMenuClick = {
//									if (!isEditingPlaylist) {
//										itemListMenuBackPressedHandler.hideAllMenus()
//										fileItemViewModel.showMenu()
//									}
//								},
//								onRemoveFromNowPlayingClick = {
//									playbackServiceController.removeFromPlaylistAtPosition(positionedFile.playlistPosition)
//								},
//								onViewFilesClick = viewFilesClickHandler,
//								onPlayClick = {
//									fileItemViewModel.hideMenu()
//									playbackServiceController.seekTo(positionedFile.playlistPosition)
//								}
//							)
//						}
//
//						if (pagerState.currentPage == 0) {
//							playingFile?.apply {
//								scope.launch {
//									reorderableState.lazyListState.scrollToItem(playlistPosition)
//								}
//							}
//						}
//
////						AndroidView(
////							factory = { context ->
////								RecyclerView(context).apply {
////									clipToOutline = true
////
////									layoutParams = RecyclerView.LayoutParams(
////										RecyclerView.LayoutParams.MATCH_PARENT,
////										RecyclerView.LayoutParams.MATCH_PARENT
////									)
////
////									layoutManager = LinearLayoutManager(context)
////
////									isNestedScrollingEnabled = true
////
////									adapter = ComposeRecyclerViewAdapter<PositionedFile>(context) { f ->
////										NowPlayingFileView(positionedFile = f)
////									}
////								}
////							},
////							modifier = Modifier
////								.weight(1f)
////								.background(SharedColors.OverlayDark),
////							{ recyclerView ->
////								recyclerView.adapter
////									?.let { it as? ComposeRecyclerViewAdapter<PositionedFile> }
////									?.updateListEventually(nowPlayingFiles)
////							}
////						)
//
//						DragDropLazyColumn(
//							dragDropListState = reorderableState,
//							modifier = Modifier.background(SharedColors.OverlayDark),
//						) {
//							dragDropItems(items = nowPlayingFiles, keyFactory = { _, f -> f }) { _, f ->
//								NowPlayingFileView(positionedFile = f)
//							}
//						}
//					}
//				}
//			}

			val isConnectionLost by connectionLostViewModel.isCheckingConnection.collectAsState()
			if (isConnectionLost) {
				AlertDialog(
					onDismissRequest = { connectionLostViewModel.cancelLibraryConnectionPolling() },
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
									connectionLostViewModel.cancelLibraryConnectionPolling()
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
	}
