package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
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

@Composable
fun NowPlayingItemView(
	itemName: String,
	artist: String,
	isActive: Boolean = false,
	isEditingPlaylist: Boolean = false,
	isHiddenMenuShown: Boolean = false,
	menuFocusRequester: FocusRequester? = null,
	onItemClick: () -> Unit = {},
	onPlayClick: () -> Unit = {},
	onViewFilesClick: () -> Unit = {},
	onHiddenMenuClick: (() -> Unit)? = null,
	onRemoveFromNowPlayingClick: (() -> Unit)? = null,
	editSideMenu: @Composable () -> Unit = {},
) {
	val hapticFeedback = LocalHapticFeedback.current

	if (!isHiddenMenuShown) {
		Row(
			modifier = Modifier
				.navigable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = onHiddenMenuClick?.run {
						{
							hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

							invoke()
						}
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = {
						if (menuFocusRequester != null && isEditingPlaylist) menuFocusRequester.requestFocus()
						else onItemClick()
					},
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
				editSideMenu()
			}
		}
	} else {
		Row(
			modifier = Modifier
				.height(rowHeight)
				.padding(Dimensions.rowPaddingValues),
			verticalAlignment = Alignment.CenterVertically,
		) {
			onRemoveFromNowPlayingClick?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.remove_item_36dp),
					contentDescription = stringResource(id = R.string.btn_remove_file),
					modifier = Modifier.clickable(onClick = it),
				)
			}

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

@Composable
fun DragDropItemScope.NowPlayingItemView(
	itemName: String,
	artist: String,
	isActive: Boolean = false,
	isEditingPlaylist: Boolean = false,
	isHiddenMenuShown: Boolean = false,
	onItemClick: () -> Unit = {},
	onPlayClick: () -> Unit = {},
	onViewFilesClick: () -> Unit = {},
	onHiddenMenuClick: (() -> Unit)? = null,
	onRemoveFromNowPlayingClick: (() -> Unit)? = null,
	onMoveItemUp: (() -> Unit)? = null,
	onMoveItemDown: (() -> Unit)? = null,
) {
	val menuFocusRequester = remember { FocusRequester() }
	NowPlayingItemView(
		itemName = itemName,
		artist = artist,
		isActive = isActive,
		isEditingPlaylist = isEditingPlaylist,
		isHiddenMenuShown = isHiddenMenuShown,
		menuFocusRequester = menuFocusRequester,
		onItemClick = onItemClick,
		onPlayClick = onPlayClick,
		onViewFilesClick = onViewFilesClick,
		onHiddenMenuClick = onHiddenMenuClick,
		onRemoveFromNowPlayingClick = onRemoveFromNowPlayingClick,
		editSideMenu = {
			onMoveItemUp?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.chevron_up_white_36dp),
					contentDescription = "Move Item Up",
					modifier = Modifier
						.navigable(
							onClick = it,
							focusRequester = menuFocusRequester
						),
				)
			}

			onMoveItemDown?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.chevron_up_white_36dp),
					contentDescription = "Move Item Down",
					modifier = Modifier
						.rotate(180f)
						.navigable(onClick = it),
				)
			}

			onRemoveFromNowPlayingClick?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.remove_item_36dp),
					contentDescription = stringResource(id = R.string.btn_remove_file),
					modifier = Modifier
						.navigable(onClick = it),
				)
			}

			ListItemIcon(
				painter = painterResource(id = R.drawable.drag),
				contentDescription = stringResource(id = R.string.drag_item),
				modifier = Modifier
					.padding(Dimensions.viewPaddingUnit)
					.detectDrag(),
			)
		}
	)
}

@Composable
fun NowPlayingItemView(
	itemName: String,
	artist: String,
	isActive: Boolean = false,
	isEditingPlaylist: Boolean = false,
	isHiddenMenuShown: Boolean = false,
	onItemClick: () -> Unit = {},
	onPlayClick: () -> Unit = {},
	onViewFilesClick: () -> Unit = {},
	onHiddenMenuClick: (() -> Unit)? = null,
	onRemoveFromNowPlayingClick: (() -> Unit)? = null,
	onMoveItemUp: (() -> Unit)? = null,
	onMoveItemDown: (() -> Unit)? = null,
) {
	val menuFocusRequester = remember { FocusRequester() }
	NowPlayingItemView(
		itemName = itemName,
		artist = artist,
		isActive = isActive,
		isEditingPlaylist = isEditingPlaylist,
		isHiddenMenuShown = isHiddenMenuShown,
		menuFocusRequester = menuFocusRequester,
		onItemClick = onItemClick,
		onPlayClick = onPlayClick,
		onViewFilesClick = onViewFilesClick,
		onHiddenMenuClick = onHiddenMenuClick,
		onRemoveFromNowPlayingClick = onRemoveFromNowPlayingClick,
		editSideMenu = {
			onMoveItemUp?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.chevron_up_white_36dp),
					contentDescription = "Move Item Up",
					modifier = Modifier
						.navigable(
							onClick = it,
							focusRequester = menuFocusRequester
						),
				)
			}

			onMoveItemDown?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.chevron_up_white_36dp),
					contentDescription = "Move Item Down",
					modifier = Modifier
						.rotate(180f)
						.navigable(onClick = it),
				)
			}

			onRemoveFromNowPlayingClick?.also {
				ListItemIcon(
					painter = painterResource(id = R.drawable.remove_item_36dp),
					contentDescription = stringResource(id = R.string.btn_remove_file),
					modifier = Modifier
						.navigable(onClick = it),
				)
			}
		}
	)
}
