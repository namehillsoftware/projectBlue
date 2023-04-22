package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingItemView(
	itemName: String,
	artist: String,
	isActive: Boolean = false,
	isEditingPlaylist: Boolean = false,
	isHiddenMenuShown: Boolean = false,
	reorderableState: ReorderableLazyListState? = null,
	onItemClick: () -> Unit = {},
	onHiddenMenuClick: () -> Unit = {},
	onRemoveFromNowPlayingClick: () -> Unit = {},
	onViewFilesClick: () -> Unit = {},
	onPlayClick: () -> Unit = {},
) {

	val hapticFeedback = LocalHapticFeedback.current
	val rowHeight = Dimensions.twoLineRowHeight

	if (!isHiddenMenuShown) {
		Row(
			modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

						onHiddenMenuClick()
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = onItemClick
				)
				.height(rowHeight)
				.fillMaxSize()
				.padding(8.dp),
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

			if (isEditingPlaylist && reorderableState != null) {
				Image(
					painter = painterResource(id = R.drawable.drag),
					contentDescription = stringResource(id = R.string.drag_item),
					modifier = Modifier
						.padding(Dimensions.viewPaddingUnit)
						.detectReorder(reorderableState),
				)
			}
		}
	} else {
		Row(modifier = Modifier
			.height(rowHeight)
			.padding(8.dp)
		) {
			Image(
				painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
				contentDescription = stringResource(id = R.string.btn_remove_file),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { onRemoveFromNowPlayingClick() }
					.align(Alignment.CenterVertically),
			)

			Image(
				painter = painterResource(id = R.drawable.ic_menu_36dp),
				contentDescription = stringResource(id = R.string.btn_view_files),
				modifier = Modifier
					.fillMaxWidth()
					.clickable { onViewFilesClick() }
					.weight(1f)
					.align(Alignment.CenterVertically),
			)

			Image(
				painter = painterResource(id = R.drawable.av_play),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { onPlayClick() }
					.align(Alignment.CenterVertically),
			)
		}
	}
}
