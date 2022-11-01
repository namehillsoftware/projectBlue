package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.compose.foundation.layout.*
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
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel

@Composable
fun SearchFilesView(
	searchFilesViewModel: SearchFilesViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	trackHeadlineViewModelProvider: TrackHeadlineViewModelProvider,
	itemListMenuViewModel: ItemListMenuViewModel
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
				itemListMenuViewModel.hideAllMenus()
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

	Surface {
		Column {
			val isLoading by searchFilesViewModel.isLoading.collectAsState()

			Row(
				modifier = Modifier.fillMaxWidth().padding(16.dp),
				horizontalArrangement = Arrangement.Center,
			) {
				val query by searchFilesViewModel.query.collectAsState()

				TextField(
					value = query,
					placeholder = { stringResource(id = R.string.lbl_search_hint) },
					onValueChange = { searchFilesViewModel.query.value = it },
					singleLine = true,
					keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
					keyboardActions = KeyboardActions(onSearch = { searchFilesViewModel.findFiles() }),
					trailingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.lbl_search)) },
					enabled = !isLoading
				)
			}

			when {
				isLoading -> {
					Box(modifier = Modifier.weight(1f).fillMaxSize()) {
						CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
					}
				}
				files.any() -> {
					LazyColumn(modifier = Modifier.weight(1f)) {
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
		}
	}
}
