package com.lasthopesoftware.bluewater.client.browsing.library

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LibraryDestinationGraphNavigation(
	private val inner: NavigateApplication,
	private val navController: NavController<BrowserLibraryDestination>,
	private val coroutineScope: CoroutineScope,
	private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) : NavigateApplication by inner {

	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		ensureBrowserIsOnStack(libraryId)

		navController.navigate(SearchScreen(libraryId))
	}.toPromise()

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> = coroutineScope.launch {
		ensureBrowserIsOnStack(libraryId)

		navController.navigate(SearchScreen(libraryId, filePropertyFilter))
	}.toPromise()

	override fun viewActiveDownloads(libraryId: LibraryId) = coroutineScope.launch {
		ensureBrowserIsOnStack(libraryId)

		navController.navigate(DownloadsScreen(libraryId))
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.navigate(LibraryScreen(libraryId))
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		navController.navigate(ItemScreen(libraryId, item))
	}.toPromise()

	override fun navigateUp() = coroutineScope.async {
		(navController.pop() && navController.backstack.entries.any()) || inner.navigateUp().suspend()
	}.toPromise()

	override fun backOut() = coroutineScope.async {
		itemListMenuBackPressedHandler.hideAllMenus() || navigateUp().suspend()
	}.toPromise()

	private suspend fun ensureBrowserIsOnStack(libraryId: LibraryId) {
		if (!navController.popUpTo { it is ItemScreen || it is LibraryScreen }) {
			viewLibrary(libraryId).suspend()
		}
	}
}
