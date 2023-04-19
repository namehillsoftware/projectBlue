package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeaderItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.ControlScreenOnState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import kotlinx.coroutines.launch

@Composable
private fun NowPlayingCoverArtView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel
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
@OptIn(ExperimentalFoundationApi::class)
fun NowPlayingView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	screenOnState: ControlScreenOnState,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
) {
	Surface {
		NowPlayingCoverArtView(nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel)

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
		val pagerState = rememberPagerState()
		VerticalPager(
			pageSize = PageSize.Fill,
			pageCount = 2,
			state = pagerState,
			modifier = Modifier
				.fillMaxSize()
				.background(colorResource(id = R.color.overlay_dark))
				.padding(systemBarsPadding),
		) { page ->
			val scope = rememberCoroutineScope()
			val filePosition by nowPlayingFilePropertiesViewModel.filePosition.collectAsState()
			val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.collectAsState()
			val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }

			if (page == 0) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = { nowPlayingFilePropertiesViewModel.showNowPlayingControls() }
						),
				) {
					val isScreenControlsVisible by nowPlayingFilePropertiesViewModel.isScreenControlsVisible.collectAsState()
					val isPlaying by nowPlayingFilePropertiesViewModel.isPlaying.collectAsState()

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Column {
							ProvideTextStyle(value = MaterialTheme.typography.h5) {
								val title by nowPlayingFilePropertiesViewModel.title.collectAsState()

								Text(text = title, color = Color.White)
							}

							ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
								val artist by nowPlayingFilePropertiesViewModel.artist.collectAsState()
								Text(text = artist, color = Color.White)
							}
						}

						if (isScreenControlsVisible) {
							Row {
								val isScreenOn by screenOnState.isScreenOnEnabled.collectAsState()
								Image(
									painter = painterResource(if (isScreenOn) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
									alpha = .8f,
									contentDescription = stringResource(if (isScreenOn) R.string.screen_is_on else R.string.screen_is_off),
									modifier = Modifier
										.padding(Dimensions.ViewPadding)
										.clickable(onClick = screenOnState::toggleScreenOn),
								)

								Image(
									painter = painterResource(R.drawable.chevron_up_white_36dp),
									alpha = .8f,
									contentDescription = stringResource(R.string.btn_view_files),
									modifier = Modifier
										.padding(Dimensions.ViewPadding)
										.clickable(onClick = {
											scope.launch {
												pagerState.scrollToPage(1)
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
							val rating by nowPlayingFilePropertiesViewModel.songRating.collectAsState()
							val ratingInt by remember { derivedStateOf { rating.toInt() } }
							RatingBar(
								rating = ratingInt,
								color = Color.White,
								backgroundColor = Color.White.copy(alpha = .1f),
								modifier = Modifier.height(52.dp).padding(bottom = 16.dp),
								onRatingSelected = { nowPlayingFilePropertiesViewModel.updateRating(it.toFloat()) }
							)
						}

						LinearProgressIndicator(
							progress = fileProgress,
							color = Color.White,
							backgroundColor = Color.White.copy(alpha = .6f),
							modifier = Modifier
								.fillMaxWidth()
								.padding(0.dp)
						)

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

								if (isPlaying) {
									Image(
										painter = painterResource(id = R.drawable.av_pause_white),
										contentDescription = stringResource(id = R.string.btn_pause),
										modifier = Modifier.clickable {
											playbackServiceController.pause()
											nowPlayingFilePropertiesViewModel.togglePlaying(false)
										}
									)
								} else {
									Image(
										painter = painterResource(id = R.drawable.av_play_white),
										contentDescription = stringResource(id = R.string.btn_play),
										modifier = Modifier.clickable {
											playbackServiceController.pause()
											nowPlayingFilePropertiesViewModel.togglePlaying(true)
										}
									)
								}

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
			} else {
				Box {
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
							val isEditingPlaylist by playlistViewModel.isEditingPlaylistState.collectAsState()
							if (isEditingPlaylist) {
								Image(
									painter = painterResource(id = R.drawable.pencil),
									contentDescription = stringResource(id = R.string.edit_now_playing_list),
									modifier = Modifier.clickable {
										playlistViewModel.editPlaylist()
									},
									alpha = .8f,
								)
							} else {
								Image(
									painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
									contentDescription = stringResource(id = R.string.finish_edit_now_playing_list),
									modifier = Modifier.clickable {
										playlistViewModel.finishPlaylistEdit()
									},
									alpha = .8f,
								)
							}

							val isRepeating by nowPlayingFilePropertiesViewModel.isRepeating.collectAsState()
							if (isRepeating) {
								Image(
									painter = painterResource(id = R.drawable.av_no_repeat_white),
									contentDescription = stringResource(id = R.string.btn_repeat_playlist),
									modifier = Modifier.clickable {
										nowPlayingFilePropertiesViewModel.toggleRepeating()
									},
									alpha = .8f,
								)
							} else {
								Image(
									painter = painterResource(id = R.drawable.av_repeat_white),
									contentDescription = stringResource(id = R.string.btn_complete_playlist),
									modifier = Modifier.clickable {
										nowPlayingFilePropertiesViewModel.toggleRepeating()
									},
									alpha = .8f,
								)
							}

							val isPlaying by nowPlayingFilePropertiesViewModel.isPlaying.collectAsState()
							if (isPlaying) {
								Image(
									painter = painterResource(id = R.drawable.av_pause_white),
									contentDescription = stringResource(id = R.string.btn_pause),
									modifier = Modifier.clickable {
										playbackServiceController.pause()
										nowPlayingFilePropertiesViewModel.togglePlaying(false)
									}
								)
							} else {
								Image(
									painter = painterResource(id = R.drawable.av_play_white),
									contentDescription = stringResource(id = R.string.btn_play),
									modifier = Modifier.clickable {
										playbackServiceController.play()
										nowPlayingFilePropertiesViewModel.togglePlaying(true)
									}
								)
							}

							Image(
								painter = painterResource(R.drawable.chevron_up_white_36dp),
								alpha = .8f,
								contentDescription = stringResource(R.string.btn_hide_files),
								modifier = Modifier
									.clickable(onClick = {
										scope.launch {
											pagerState.scrollToPage(1)
										}
									})
									.rotate(180f),
							)
						}

						LinearProgressIndicator(
							progress = fileProgress,
							color = Color.White,
							backgroundColor = Color.White.copy(alpha = .6f),
							modifier = Modifier
								.fillMaxWidth()
								.padding(0.dp)
						)


						val nowPlayingFiles by playlistViewModel.nowPlayingList.collectAsState()
						val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.collectAsState()

						@Composable
						fun NowPlayingFileView(positionedFile: PositionedFile) {
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
							val isPlaying by remember { derivedStateOf { playingFile == positionedFile } }

							val viewFilesClickHandler = {
								nowPlayingFilePropertiesViewModel.activeLibraryId?.also {
									applicationNavigation.viewFileDetails(
										it,
										nowPlayingFiles.map { p -> p.serviceFile },
										positionedFile.playlistPosition
									)
								}
								Unit
							}

							TrackHeaderItemView(
								itemName = fileName,
								isActive = isPlaying,
								isHiddenMenuShown = isMenuShown,
								onItemClick = viewFilesClickHandler,
								onHiddenMenuClick = {
									itemListMenuBackPressedHandler.hideAllMenus()
									fileItemViewModel.showMenu()
								},
								onAddToNowPlayingClick = {
									playbackServiceController.addToPlaylist(positionedFile.serviceFile)
								},
								onViewFilesClick = viewFilesClickHandler,
								onPlayClick = {
									fileItemViewModel.hideMenu()
									playbackServiceController.startPlaylist(
										nowPlayingFiles.map { it.serviceFile },
										positionedFile.playlistPosition
									)
								}
							)
						}

						LazyColumn(
							modifier = Modifier.weight(1f)
						) {
							items(nowPlayingFiles) { f ->
								NowPlayingFileView(f)
							}
						}
					}
				}
			}
		}
	}
}
