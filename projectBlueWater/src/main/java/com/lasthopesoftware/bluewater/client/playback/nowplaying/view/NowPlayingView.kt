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
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.ControlScreenOnState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NowPlayingView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	screenOnState: ControlScreenOnState,
) {
	Surface {
		val pagerState = rememberPagerState()
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
				coverArt
					?.let {
						val coverArtBitmap by remember { derivedStateOf { it.asImageBitmap() } }
						Image(
							bitmap = coverArtBitmap,
							contentDescription = stringResource(id = R.string.img_now_playing),
							contentScale = ContentScale.Crop,
							alignment = Alignment.Center,
						)
					}
			}
		}

		VerticalPager(
			pageCount = 2,
			state = pagerState,
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Transparent),
		) {
			Box {
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
		}
	}
}
