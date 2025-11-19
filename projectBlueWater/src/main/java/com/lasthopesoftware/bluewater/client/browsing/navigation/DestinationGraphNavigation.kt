package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
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

class DestinationGraphNavigation(
    private val inner: NavigateApplication,
    private val navController: NavController<Destination>,
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

	override fun viewApplicationSettings() = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }
	}.toPromise()

	override fun viewHiddenSettings(): Promise<Unit> = coroutineScope.launch {
		navController.navigate(HiddenSettingsScreen)
	}.toPromise()

	override fun viewNewServerSettings() = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(NewConnectionSettingsScreen)
	}.toPromise()

	override fun viewServerSettings(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen || it is LibraryScreen }
		navController.navigate(ConnectionSettingsScreen(libraryId))
	}.toPromise()

	override fun viewActiveDownloads(libraryId: LibraryId) = coroutineScope.launch {
		ensureBrowserIsOnStack(libraryId)

		navController.navigate(DownloadsScreen(libraryId))
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))
	}.toPromise()

	override fun viewFileDetails(libraryId: LibraryId, searchQuery: String, positionedFile: PositionedFile) = coroutineScope.launch {
		navController.navigate(SearchedFileDetailsScreen(libraryId, searchQuery, positionedFile))
	}.toPromise()

	override fun viewFileDetails(libraryId: LibraryId, item: IItem?, positionedFile: PositionedFile) = coroutineScope.launch {
		navController.navigate(BrowsedFileDetailsScreen(libraryId, item, positionedFile))
	}.toPromise()

	override fun viewNowPlayingFileDetails(libraryId: LibraryId, positionedFile: PositionedFile) = coroutineScope.launch {
		navController.navigate(FileDetailsFromNowPlayingScreen(libraryId, positionedFile))
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		navController.navigate(ItemScreen(libraryId, item))
	}.toPromise()

	override fun viewNowPlaying(libraryId: LibraryId) = coroutineScope.launch {
		if (!navController.popUpTo { it is NowPlayingScreen }) {
			ensureBrowserIsOnStack(libraryId)
			navController.navigate(NowPlayingScreen(libraryId))
		}
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
