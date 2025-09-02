package com.lasthopesoftware.bluewater.client.browsing.items.list.menus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.LabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.LoadedLibraryState
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel

@Composable
fun LabelledActiveDownloadsButton(
	loadedLibraryState: LoadedLibraryState,
	applicationNavigation: NavigateApplication,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null,
) {
	ColumnMenuIcon(
		onClick = {
			loadedLibraryState.loadedLibraryId?.also {
				applicationNavigation.viewActiveDownloads(it)
			}
		},
		iconPainter = painterResource(id = R.drawable.ic_water),
		contentDescription = stringResource(id = R.string.activeDownloads),
		label = stringResource(id = R.string.downloads), // Use shortened version for button size
		labelMaxLines = 1,
		modifier = modifier,
		focusRequester = focusRequester,
	)
}

@Composable
fun LabelledSearchButton(
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
		modifier = modifier,
	)
}

@Composable
fun LabelledSettingsButton(
	loadedLibraryState: LoadedLibraryState,
	applicationNavigation: NavigateApplication,
	modifier: Modifier = Modifier,
) {
	val settingsButtonLabel = stringResource(id = R.string.settings)
	ColumnMenuIcon(
		onClick = {
			loadedLibraryState.loadedLibraryId?.also(applicationNavigation::viewServerSettings)
		},
		iconPainter = painterResource(id = R.drawable.ic_action_settings),
		contentDescription = settingsButtonLabel,
		label = settingsButtonLabel,
		labelMaxLines = 1,
		modifier = modifier,
	)
}

@Composable
fun LabelledRefreshButton(
	itemDataLoader: LoadItemData,
	modifier: Modifier = Modifier,
) {
	LabelledRefreshButton(
		onClick = {
			itemDataLoader.promiseRefresh()
		},
		modifier = modifier,
	)
}
