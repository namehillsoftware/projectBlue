package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackTitleItemView(
	itemName: String,
	isActive: Boolean = false,
	isHiddenMenuShown: Boolean = false,
	onItemClick: () -> Unit = {},
	onHiddenMenuClick: () -> Unit = {},
	onAddToNowPlayingClick: () -> Unit = {},
	onViewFilesClick: () -> Unit = {},
	onPlayClick: () -> Unit = {},
) {

	val hapticFeedback = LocalHapticFeedback.current
	val rowHeight = Dimensions.standardRowHeight
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }

	if (!isHiddenMenuShown) {
		Box(modifier = Modifier
			.navigable(
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
			.fillMaxSize(),
		) {
			Text(
				text = itemName,
				fontSize = rowFontSize,
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
				fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
				modifier = Modifier
					.padding(12.dp)
					.align(Alignment.CenterStart),
			)
		}
	} else {
		Row(modifier = Modifier
			.height(rowHeight)
			.padding(8.dp)
		) {
			ListItemIcon(
				painter = painterResource(id = R.drawable.ic_add_item_36dp),
				contentDescription = stringResource(id = R.string.btn_add_file_to_playback),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { onAddToNowPlayingClick() }
					.align(Alignment.CenterVertically),
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.ic_menu_36dp),
				contentDescription = stringResource(id = R.string.btn_view_files),
				modifier = Modifier
					.fillMaxWidth()
					.clickable { onViewFilesClick() }
					.weight(1f)
					.align(Alignment.CenterVertically),
			)

			ListItemIcon(
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
