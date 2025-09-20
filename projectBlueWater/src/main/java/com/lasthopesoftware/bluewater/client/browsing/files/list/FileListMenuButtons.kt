package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor
import com.lasthopesoftware.bluewater.android.ui.theme.SharedAlphas
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

@Composable
fun LabelledShuffleButton(
	libraryState: LoadedLibraryState,
	playbackServiceController: ControlPlaybackService,
	serviceFilesListState: ServiceFilesListState,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
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
		labelMaxLines = 1,
		modifier = modifier,
		enabled = enabled,
	)
}

@Composable
fun LabelledPlayButton(
	libraryState: LoadedLibraryState,
	playbackServiceController: ControlPlaybackService,
	serviceFilesListState: ServiceFilesListState,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	focusRequester: FocusRequester? = null,
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
		labelMaxLines = 1,
		modifier = modifier,
		enabled = enabled,
		focusRequester = focusRequester,
	)
}

@Composable
fun LabelledSyncButton(
	fileListViewModel: FileListViewModel,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
) {
	val isSynced by fileListViewModel.isSynced.subscribeAsState()
	val syncButtonLabel =
		if (!isSynced) stringResource(id = R.string.btn_sync_item)
		else stringResource(id = R.string.files_synced)
	var syncColor = if (isSynced) MaterialTheme.colors.primary else LocalControlColor.current
	if (!enabled)
		syncColor = syncColor.copy(alpha = SharedAlphas.disabledAlpha)
	ColumnMenuIcon(
		onClick = { fileListViewModel.toggleSync() },
		icon = {
			SyncIcon(
				isActive = isSynced,
				modifier = Modifier.size(Dimensions.topMenuIconSize),
				contentDescription = syncButtonLabel,
				tint = syncColor,
			)
		},
		label = syncButtonLabel,
		labelMaxLines = 1,
		modifier = modifier,
		enabled = enabled,
	)
}
