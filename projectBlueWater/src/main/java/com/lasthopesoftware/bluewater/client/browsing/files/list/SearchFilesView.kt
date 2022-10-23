package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel

@Composable
fun SearchFilesView(
	searchFilesViewModel: SearchFilesViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	trackHeadlineViewModelProvider: TrackHeadlineViewModelProvider,
) {
	val files by searchFilesViewModel.files.collectAsState()
	val playingFile by nowPlayingViewModel.nowPlayingFile.collectAsState()

	@Composable
	fun RenderTrackHeaderItem(position: Int, serviceFile: ServiceFile) {
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

		DisposableEffect(Unit) {
			fileItemViewModel.promiseUpdate(files, position)

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
		val fileName by fileItemViewModel.title.collectAsState()

		TrackHeaderItem(
			itemName = fileName,
			isPlaying = playingFile?.serviceFile == serviceFile,
			isMenuShown = isMenuShown,
			onItemClick = fileItemViewModel::viewFileDetails,
			onHiddenMenuClick = {
				fileItemViewModel.showMenu()
			},
			onAddToNowPlayingClick = fileItemViewModel::addToNowPlaying,
			onViewFilesClick = fileItemViewModel::viewFileDetails,
			onPlayClick = {
				fileItemViewModel.hideMenu()
				searchFilesViewModel.play(position)
			}
		)
	}

	LazyColumn {
		item {
			Row {
				val query by searchFilesViewModel.query.collectAsState()

				TextField(
					value = query,
					onValueChange = { searchFilesViewModel.query.value = it },
					modifier = Modifier.weight(1f),
					singleLine = true,
					keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
					keyboardActions = KeyboardActions(onSearch = { searchFilesViewModel.findFiles() }),
					trailingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.lbl_search)) },
				)
			}
		}

		if (!files.any()) return@LazyColumn

		item {
			Box(
				modifier = Modifier
					.padding(4.dp)
					.height(48.dp)
			) {
				ProvideTextStyle(MaterialTheme.typography.h5) {
					Text(
						text = "${files.size} files",
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
