package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayPauseButton(
    nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
    playbackServiceController: ControlPlaybackService,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
	val isPlaying by nowPlayingFilePropertiesViewModel.isPlaying.subscribeAsState()
	Image(
		painter = painterResource(id = if (isPlaying) R.drawable.av_pause_white else R.drawable.av_play_white),
		contentDescription = stringResource(id = if (isPlaying) R.string.btn_pause else R.string.btn_play),
		modifier = modifier.navigable(
			interactionSource = remember { MutableInteractionSource() },
			indication = null,
			onClick = {
				if (isPlaying) playbackServiceController.pause()
				else nowPlayingFilePropertiesViewModel.activeLibraryId.value?.also(playbackServiceController::play)
				nowPlayingFilePropertiesViewModel.togglePlaying(!isPlaying)
			}),
		alpha = alpha,
	)
}
