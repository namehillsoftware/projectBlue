package com.lasthopesoftware.bluewater.client.browsing.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.PlayPauseButton
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import kotlinx.coroutines.launch

private val bottomAppBarHeight = Dimensions.appBarHeight

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun LibraryMenu(
	applicationNavigation: NavigateApplication,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	bottomSheetState: BottomSheetState,
	libraryId: LibraryId,
) {
	val nowPlayingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.collectAsState()
	val isNowPlayingFileSet by remember { derivedStateOf { nowPlayingFile != null } }

	var rowModifier = Modifier
		.background(MaterialTheme.colors.secondary)
		.height(bottomAppBarHeight)

	if (isNowPlayingFileSet)
		rowModifier = rowModifier.clickable(onClick = applicationNavigation::viewNowPlaying)

	Row(
		modifier = rowModifier
	) {
		Column {
			Row(
				modifier = Modifier
					.weight(1f)
					.padding(end = 16.dp)
			) {
				val progress by remember {
					derivedStateOf {
						if (bottomSheetState.currentValue == BottomSheetValue.Collapsed) 0f else 1f
					}
				}

				val coroutineScope = rememberCoroutineScope()
				Box(
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.fillMaxHeight()
						.wrapContentWidth()
						.clickable {
							coroutineScope.launch {
								if (bottomSheetState.isExpanded) bottomSheetState.collapse()
								else bottomSheetState.expand()
							}
						},
				) {
					val chevronRotation by remember { derivedStateOf { 180 * progress } }
					Image(
						painter = painterResource(id = R.drawable.chevron_up_white_36dp),
						contentDescription = stringResource(id = if (bottomSheetState.isCollapsed) R.string.show_menu else R.string.hide_menu),
						modifier = Modifier
							.align(Alignment.Center)
							.rotate(chevronRotation)
							.padding(start = 16.dp, end = 16.dp)
							.requiredSize(24.dp)
					)
				}

				Column(
					modifier = Modifier
						.weight(1f)
						.align(Alignment.CenterVertically),
				) {
					val songTitle by nowPlayingFilePropertiesViewModel.title.collectAsState()

					ProvideTextStyle(MaterialTheme.typography.subtitle1) {
						Text(
							text = songTitle,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							fontWeight = FontWeight.Medium,
							color = MaterialTheme.colors.onSecondary,
						)
					}

					val songArtist by nowPlayingFilePropertiesViewModel.artist.collectAsState()
					ProvideTextStyle(MaterialTheme.typography.body2) {
						Text(
							text = songArtist,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							color = MaterialTheme.colors.onSecondary,
						)
					}
				}

				if (isNowPlayingFileSet) {
					PlayPauseButton(
						nowPlayingFilePropertiesViewModel,
						playbackServiceController,
						modifier = Modifier
							.padding(start = 8.dp, end = 8.dp)
							.align(Alignment.CenterVertically)
							.size(24.dp),
					)

					Icon(
						Icons.Default.ArrowForward,
						contentDescription = stringResource(id = R.string.title_activity_view_now_playing),
						tint = MaterialTheme.colors.onSecondary,
						modifier = Modifier
							.padding(start = 8.dp, end = 8.dp)
							.align(Alignment.CenterVertically)
					)
				}
			}

			val filePosition by nowPlayingFilePropertiesViewModel.filePosition.collectAsState()
			val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.collectAsState()
			val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }
			LinearProgressIndicator(
				progress = fileProgress,
				color = MaterialTheme.colors.primary,
				backgroundColor = MaterialTheme.colors.onPrimary.copy(alpha = .6f),
				modifier = Modifier
					.fillMaxWidth()
					.padding(0.dp)
			)
		}
	}

	val rowHeight = Dimensions.standardRowHeight
	ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
		Row(
			modifier = Modifier
				.height(rowHeight)
				.fillMaxWidth()
				.clickable {
					applicationNavigation.viewActiveDownloads(libraryId)
				},
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.align(Alignment.CenterVertically)
					.fillMaxHeight()
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_water),
					contentDescription = stringResource(id = R.string.activeDownloads),
					modifier = Modifier
						.align(Alignment.Center)
						.padding(start = 16.dp, end = 16.dp)
				)
			}

			Text(
				text = stringResource(R.string.activeDownloads),
			)
		}

		Row(
			modifier = Modifier
				.height(rowHeight)
				.fillMaxWidth()
				.clickable {
					applicationNavigation.launchSearch(libraryId)
				},
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.align(Alignment.CenterVertically)
					.fillMaxHeight()
			) {
				Icon(
					Icons.Default.Search,
					contentDescription = stringResource(id = R.string.search),
					modifier = Modifier
						.align(Alignment.Center)
						.padding(start = 16.dp, end = 16.dp)
				)
			}

			Text(
				text = stringResource(R.string.search),
			)
		}

		Row(
			modifier = Modifier
				.height(rowHeight)
				.fillMaxWidth()
				.clickable {
					applicationNavigation.viewServerSettings(libraryId)
				},
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.align(Alignment.CenterVertically)
					.fillMaxHeight()
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_action_settings),
					contentDescription = stringResource(id = R.string.settings),
					modifier = Modifier
						.align(Alignment.Center)
						.padding(start = 16.dp, end = 16.dp)
						.size(Dimensions.menuIconSize)
				)
			}

			Text(
				text = stringResource(R.string.settings),
			)
		}
	}
}
