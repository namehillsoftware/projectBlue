package com.lasthopesoftware.bluewater.client.browsing.items.list.menus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.UnlabelledSyncButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabelledRefreshButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.UnlabelledRefreshButton
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.LocalControlColor

@Composable
fun RowScope.LabelledActiveDownloadsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
	modifier: Modifier = Modifier,
) {
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				applicationNavigation.viewActiveDownloads(it)
			}
		},
		iconPainter = painterResource(id = R.drawable.ic_water),
		contentDescription = stringResource(id = R.string.activeDownloads),
		label = stringResource(id = R.string.downloads), // Use shortened version for button size
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
fun RowScope.UnlabelledActiveDownloadsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				applicationNavigation.viewActiveDownloads(it)
			}
		},
		iconPainter = painterResource(id = R.drawable.ic_water),
		contentDescription = stringResource(id = R.string.activeDownloads),
	)
}

@Composable
fun RowScope.LabelledSearchButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
	modifier: Modifier = Modifier,
) {
	val searchButtonLabel = stringResource(id = R.string.search)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::launchSearch)
		},
		iconPainter = painterResource(id = R.drawable.search_36dp),
		contentDescription = searchButtonLabel,
		label = searchButtonLabel,
		labelMaxLines = 1,
		labelModifier = modifier,
	)
}

@Composable
fun RowScope.UnlabelledSearchButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	val searchButtonLabel = stringResource(id = R.string.search)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::launchSearch)
		},
		iconPainter = painterResource(id = R.drawable.search_36dp),
		contentDescription = searchButtonLabel,
	)
}

@Composable
fun RowScope.LabelledSettingsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
	modifier: Modifier = Modifier,
) {
	val settingsButtonLabel = stringResource(id = R.string.settings)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::viewServerSettings)
		},
		iconPainter = painterResource(id = R.drawable.ic_action_settings),
		contentDescription = settingsButtonLabel,
		label = settingsButtonLabel,
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
fun RowScope.UnlabelledSettingsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	val settingsButtonLabel = stringResource(id = R.string.settings)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::viewServerSettings)
		},
		iconPainter = painterResource(id = R.drawable.ic_action_settings),
		contentDescription = settingsButtonLabel,
	)
}


@Composable
fun RowScope.LabelledRefreshButton(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	modifier: Modifier = Modifier,
) {
	LabelledRefreshButton(
		onClick = {
			itemListViewModel.promiseRefresh()
			fileListViewModel.promiseRefresh()
		},
		modifier = modifier,
	)
}

@Composable
fun RowScope.UnlabelledRefreshButton(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
) {
	UnlabelledRefreshButton {
		itemListViewModel.promiseRefresh()
		fileListViewModel.promiseRefresh()
	}
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun BoxScope.MoreFileOptionsMenu(fileListViewModel: FileListViewModel) {
	Box(modifier = Modifier
		.fillMaxSize()
		.wrapContentSize(Alignment.TopEnd)
		.align(Alignment.TopEnd)
	) {
		var isExpanded by remember { mutableStateOf(false) }
		Icon(
			painter = painterResource(R.drawable.more_vertical_24),
			contentDescription = stringResource(R.string.view_more_options),
			modifier = Modifier
				.padding(Dimensions.topRowOuterPadding)
				.navigable(onClick = { isExpanded = !isExpanded }),
			tint = LocalControlColor.current,
		)

		DropdownMenu(
			expanded = isExpanded,
			onDismissRequest = { isExpanded = false }
		) {
			DropdownMenuItem(onClick = { fileListViewModel.toggleSync() }) {
				val isSynced by fileListViewModel.isSynced.collectAsState()
				val syncButtonLabel =
					if (!isSynced) stringResource(id = R.string.btn_sync_item)
					else stringResource(id = R.string.files_synced)
				Text(syncButtonLabel)
				UnlabelledSyncButton(fileListViewModel = fileListViewModel)
			}
		}
	}
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun BoxScope.MoreItemsOnlyOptionsMenu(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	Box(modifier = Modifier
		.fillMaxSize()
		.wrapContentSize(Alignment.TopEnd)
		.align(Alignment.TopEnd)
	) {
		var isExpanded by remember { mutableStateOf(false) }
		Icon(
			painter = painterResource(R.drawable.more_vertical_24),
			contentDescription = stringResource(R.string.view_more_options),
			modifier = Modifier
				.padding(Dimensions.topRowOuterPadding)
				.navigable(onClick = { isExpanded = !isExpanded }),
			tint = LocalControlColor.current,
		)

		DropdownMenu(
			expanded = isExpanded,
			onDismissRequest = { isExpanded = false }
		) {
			DropdownMenuItem(onClick = { itemListViewModel.loadedLibraryId?.also(applicationNavigation::viewServerSettings) }) {
				val settingsButtonLabel = stringResource(id = R.string.settings)
				Text(settingsButtonLabel)
				UnlabelledSettingsButton(
					itemListViewModel = itemListViewModel,
					applicationNavigation = applicationNavigation,
				)
			}
		}
	}
}