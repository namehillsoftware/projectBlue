package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.rowScrollPadding

private val rowHeight = Dimensions.twoLineRowHeight

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NowPlayingTvItemView(
	itemName: String,
	artist: String,
	isActive: Boolean = false,
	isEditingPlaylist: Boolean = false,
	onMoveItemUp: () -> Unit = {},
	onMoveItemDown: () -> Unit = {},
	onItemClick: () -> Unit = {},
	onRemoveFromNowPlayingClick: () -> Unit = {},
) {
	val menuFocusRequester = remember { FocusRequester() }
	Row(
		modifier = Modifier
			.navigable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClickLabel = stringResource(id = R.string.btn_view_song_details),
				onClick = {
					if (isEditingPlaylist) menuFocusRequester.requestFocus()
					else onItemClick()
				},
				focusedBorderColor = Color.White,
				unfocusedBorderColor = Color.Transparent,
				scrollPadding = rowHeight.rowScrollPadding,
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
			ListItemIcon(
				painter = painterResource(id = R.drawable.chevron_up_white_36dp),
				contentDescription = "Move Item Up",
				modifier = Modifier
					.navigable(
						onClick = onMoveItemUp,
						focusRequester = menuFocusRequester
					),
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.chevron_up_white_36dp),
				contentDescription = "Move Item Down",
				modifier = Modifier
					.rotate(180f)
					.navigable(onClick = onMoveItemDown),
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
				contentDescription = stringResource(id = R.string.btn_remove_file),
				modifier = Modifier
					.navigable(onClick = onRemoveFromNowPlayingClick),
			)
		}
	}
}
