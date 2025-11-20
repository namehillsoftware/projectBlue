package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropItemScope
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions

private val rowHeight = Dimensions.twoLineRowHeight

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragDropItemScope.NowPlayingItemView(
	itemName: String,
	artist: String,
	isActive: Boolean = false,
	isEditingPlaylist: Boolean = false,
	isHiddenMenuShown: Boolean = false,
	onItemClick: () -> Unit = {},
	onHiddenMenuClick: () -> Unit = {},
	onRemoveFromNowPlayingClick: () -> Unit = {},
	onViewFilesClick: () -> Unit = {},
	onPlayClick: () -> Unit = {},
) {

	val hapticFeedback = LocalHapticFeedback.current

	if (!isHiddenMenuShown) {
		Row(
			modifier = Modifier
				.navigable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

						onHiddenMenuClick()
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = onItemClick,
					focusedBorderColor = Color.White,
					unfocusedBorderColor = Color.Transparent,
				)
				.height(rowHeight)
				.fillMaxSize()
				.padding(Dimensions.rowPaddingValues),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Column(
				modifier = Modifier.weight(1f),
			) {
				ProvideTextStyle(value = MaterialTheme.typography.h6) {
					Text(
						text = itemName,
						overflow = TextOverflow.Ellipsis,
						maxLines = 1,
						fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
					)
				}

				ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
					Text(
						text = artist,
						overflow = TextOverflow.Ellipsis,
						maxLines = 1,
					)
				}
			}

			if (isEditingPlaylist) {
				Image(
					painter = painterResource(id = R.drawable.drag),
					contentDescription = stringResource(id = R.string.drag_item),
					modifier = Modifier
						.padding(Dimensions.viewPaddingUnit)
						.detectDrag(),
				)
			}
		}
	} else {
		Row(
			modifier = Modifier
				.height(rowHeight)
				.padding(Dimensions.rowPaddingValues),
			verticalAlignment = Alignment.CenterVertically,
		) {
			ListItemIcon(
				painter = painterResource(id = R.drawable.remove_item_36dp),
				contentDescription = stringResource(id = R.string.btn_remove_file),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { onRemoveFromNowPlayingClick() },
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.ic_menu_36dp),
				contentDescription = stringResource(id = R.string.btn_view_files),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { onViewFilesClick() },
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.av_play_white),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { onPlayClick() },
			)
		}
	}
}
