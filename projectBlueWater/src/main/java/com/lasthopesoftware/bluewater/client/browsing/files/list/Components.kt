package com.lasthopesoftware.bluewater.client.browsing.files.li

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.LoadedLibraryState
import com.lasthopesoftware.bluewater.client.browsing.files.list.ServiceFilesListState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon

@Composable
fun RowScope.LabelledShuffleButton(
	libraryState: LoadedLibraryState,
	playbackServiceController: ControlPlaybackService,
	serviceFilesListState: ServiceFilesListState,
	modifier: Modifier = Modifier,
) {
	val shuffleButtonLabel = stringResource(R.string.btn_shuffle_files)
	ColumnMenuIcon(
		onClick = {
			libraryState.loadedLibraryId?.also {
				playbackServiceController.shuffleAndStartPlaylist(it, serviceFilesListState.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_shuffle),
		contentDescription = shuffleButtonLabel,
		label = shuffleButtonLabel,
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
fun RowScope.UnlabelledShuffleButton(
	libraryState: LoadedLibraryState,
	playbackServiceController: ControlPlaybackService,
	serviceFilesListState: ServiceFilesListState,
) {
	val shuffleButtonLabel = stringResource(R.string.btn_shuffle_files)
	ColumnMenuIcon(
		onClick = {
			libraryState.loadedLibraryId?.also {
				playbackServiceController.shuffleAndStartPlaylist(it, serviceFilesListState.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_shuffle),
		contentDescription = shuffleButtonLabel,
	)
}

@Composable
fun RowScope.LabelledPlayButton(
	libraryState: LoadedLibraryState,
	playbackServiceController: ControlPlaybackService,
	serviceFilesListState: ServiceFilesListState,
	modifier: Modifier = Modifier,
) {
	val playButtonLabel = stringResource(id = R.string.btn_play)
	ColumnMenuIcon(
		onClick = {
			libraryState.loadedLibraryId?.also {
				playbackServiceController.startPlaylist(it, serviceFilesListState.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_play),
		contentDescription = playButtonLabel,
		label = playButtonLabel,
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
fun RowScope.UnlabelledPlayButton(
	libraryState: LoadedLibraryState,
	playbackServiceController: ControlPlaybackService,
	serviceFilesListState: ServiceFilesListState,
) {
	val playButtonLabel = stringResource(id = R.string.btn_play)
	ColumnMenuIcon(
		onClick = {
			libraryState.loadedLibraryId?.also {
				playbackServiceController.startPlaylist(it, serviceFilesListState.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_play),
		contentDescription = playButtonLabel,
		label = null,
	)
}
