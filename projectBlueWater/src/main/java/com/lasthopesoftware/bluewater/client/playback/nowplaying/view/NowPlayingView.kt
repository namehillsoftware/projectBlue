package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.ControlScreenOnState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import kotlinx.coroutines.launch

@Composable
fun NowPlayingCoverArtView(
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
	playbackServiceController: ControlPlaybackService
) {
	Surface {
		NowPlayingCoverArtView(nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel)

		val pagerState = rememberPagerState()
		VerticalPager(
			pageCount = 2,
			state = pagerState,
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Transparent),
		) {
			Box {
				val isScreenControlsVisible by nowPlayingFilePropertiesViewModel.isScreenControlsVisible.collectAsState()

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Column {
						ProvideTextStyle(value = MaterialTheme.typography.h5) {
							val title by nowPlayingFilePropertiesViewModel.title.collectAsState()

							Text(text = title)
						}

						ProvideTextStyle(value = MaterialTheme.typography.h4) {
							val artist by nowPlayingFilePropertiesViewModel.artist.collectAsState()
							Text(text = artist)
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

							val scope = rememberCoroutineScope()
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
							modifier = Modifier.height(TextFieldDefaults.MinHeight),
							onRatingSelected = { nowPlayingFilePropertiesViewModel.updateRating(it.toFloat()) }
						)
					}

					val filePosition by nowPlayingFilePropertiesViewModel.filePosition.collectAsState()
					val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.collectAsState()
					val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }
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
					) {
						if (isScreenControlsVisible) {
							Image(
								painter = painterResource(id = R.drawable.av_previous_white),
								contentDescription = stringResource(id = R.string.btn_previous),
								modifier = Modifier.clickable {
									playbackServiceController.previous()
								}
							)

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
		}
	}
}
