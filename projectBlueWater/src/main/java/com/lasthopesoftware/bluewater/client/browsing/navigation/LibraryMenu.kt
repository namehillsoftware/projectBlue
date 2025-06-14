package com.lasthopesoftware.bluewater.client.browsing.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.PlayPauseButton
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import kotlinx.coroutines.launch

private val bottomAppBarHeight = Dimensions.appBarHeight
private val iconPaddingDp = Dimensions.viewPaddingUnit * 4
private val iconPadding = PaddingValues(start = iconPaddingDp, end = iconPaddingDp)
private val hiddenMenuRowHeight = Dimensions.standardRowHeight

fun Modifier.iconModifier(scope: BoxScope) = with (scope) {
	then(
		Modifier.align(Alignment.Center)
			.padding(iconPadding)
			.size(Dimensions.topMenuIconSize)
	)
}

@Composable
fun LibraryMenu(
    applicationNavigation: NavigateApplication,
    nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
    playbackServiceController: ControlPlaybackService,
    bottomSheetState: BottomSheetState,
    libraryId: LibraryId,
) {
	val nowPlayingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.subscribeAsState()
	val isNowPlayingFileSet by remember { derivedStateOf { nowPlayingFile != null } }

	var rowModifier = Modifier
		.background(MaterialTheme.colors.secondary)
		.height(bottomAppBarHeight)

	if (isNowPlayingFileSet)
		rowModifier = rowModifier.clickable(onClick = { applicationNavigation.viewNowPlaying(libraryId) })

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
						bottomSheetState.progress(BottomSheetValue.Collapsed, BottomSheetValue.Expanded)
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
					val songTitle by nowPlayingFilePropertiesViewModel.title.subscribeAsState()

					ProvideTextStyle(MaterialTheme.typography.subtitle1) {
						Text(
							text = songTitle,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							fontWeight = FontWeight.Medium,
							color = MaterialTheme.colors.onSecondary,
						)
					}

					val songArtist by nowPlayingFilePropertiesViewModel.artist.subscribeAsState()
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
							.size(Dimensions.topMenuIconSize),
					)

					Icon(
						painterResource(R.drawable.arrow_right_24dp),
						contentDescription = stringResource(id = R.string.title_activity_view_now_playing),
						tint = MaterialTheme.colors.onSecondary,
						modifier = Modifier
							.padding(start = 8.dp, end = 8.dp)
							.align(Alignment.CenterVertically)
					)
				}
			}

			val filePosition by nowPlayingFilePropertiesViewModel.filePosition.subscribeAsState()
			val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.subscribeAsState()
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

	ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
		CompositionLocalProvider(
			LocalContentAlpha provides ContentAlpha.medium
		) {
			Row(
				modifier = Modifier
					.height(hiddenMenuRowHeight)
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
					Icon(
						painter = painterResource(id = R.drawable.ic_water),
						contentDescription = stringResource(id = R.string.activeDownloads),
						modifier = Modifier.iconModifier(this)
					)
				}

				Text(
					text = stringResource(R.string.activeDownloads),
				)
			}

			Row(
				modifier = Modifier
					.height(hiddenMenuRowHeight)
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
						painter = painterResource(id = R.drawable.search_36dp),
						contentDescription = stringResource(id = R.string.search),
						modifier = Modifier.iconModifier(this)
					)
				}

				Text(
					text = stringResource(R.string.search),
				)
			}

			Row(
				modifier = Modifier
					.height(hiddenMenuRowHeight)
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
					Icon(
						painter = painterResource(id = R.drawable.ic_action_settings),
						contentDescription = stringResource(id = R.string.settings),
						modifier = Modifier.iconModifier(this)
					)
				}

				Text(
					text = stringResource(R.string.settings),
				)
			}
		}
	}
}
