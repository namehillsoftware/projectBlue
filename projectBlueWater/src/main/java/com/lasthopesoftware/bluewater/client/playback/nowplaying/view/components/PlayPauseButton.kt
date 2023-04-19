package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService

@Composable
fun PlayPauseButton(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	modifier: Modifier = Modifier,
) {
	val isPlaying by nowPlayingFilePropertiesViewModel.isPlaying.collectAsState()
	if (isPlaying) {
		Image(
			painter = painterResource(id = R.drawable.av_pause_white),
			contentDescription = stringResource(id = R.string.btn_pause),
			modifier = modifier.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = {
					playbackServiceController.pause()
					nowPlayingFilePropertiesViewModel.togglePlaying(false)
				})
		)
	} else {
		Image(
			painter = painterResource(id = R.drawable.av_play_white),
			contentDescription = stringResource(id = R.string.btn_play),
			modifier = modifier.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = {
					playbackServiceController.play()
					nowPlayingFilePropertiesViewModel.togglePlaying(true)
				})
		)
	}
}
