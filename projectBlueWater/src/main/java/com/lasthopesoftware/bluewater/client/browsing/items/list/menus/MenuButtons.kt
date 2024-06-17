package com.lasthopesoftware.bluewater.client.browsing.items.list.menus

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions

@Composable
fun RowScope.LabelledSyncButton(
	fileListViewModel: FileListViewModel,
	modifier: Modifier = Modifier,
) {
	val isSynced by fileListViewModel.isSynced.collectAsState()
	val syncButtonLabel =
		if (!isSynced) stringResource(id = R.string.btn_sync_item)
		else stringResource(id = R.string.files_synced)
	ColumnMenuIcon(
		onClick = { fileListViewModel.toggleSync() },
		icon = {
			SyncIcon(
				isActive = isSynced,
				modifier = Modifier.size(Dimensions.topMenuIconSize),
				contentDescription = syncButtonLabel,
			)
		},
		label = syncButtonLabel,
		labelMaxLines = 1,
		labelModifier = modifier,
	)
}

@Composable
fun RowScope.UnlabelledSyncButton(fileListViewModel: FileListViewModel) {
	val isSynced by fileListViewModel.isSynced.collectAsState()
	val syncButtonLabel =
		if (!isSynced) stringResource(id = R.string.btn_sync_item)
		else stringResource(id = R.string.files_synced)
	ColumnMenuIcon(
		onClick = { fileListViewModel.toggleSync() },
		icon = {
			SyncIcon(
				isActive = isSynced,
				modifier = Modifier.size(Dimensions.topMenuIconSize),
				contentDescription = syncButtonLabel,
			)
		},
	)
}

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
