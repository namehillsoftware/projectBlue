package com.lasthopesoftware.bluewater.client.browsing.items.list.menus

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.UnlabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel

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
fun UnlabelledRefreshButton(
	itemDataLoader: LoadItemData,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null,
) {
	UnlabelledRefreshButton(
		onClick = {
			itemDataLoader.promiseRefresh()
		},
		modifier = modifier,
		focusRequester = focusRequester,
	)
}
