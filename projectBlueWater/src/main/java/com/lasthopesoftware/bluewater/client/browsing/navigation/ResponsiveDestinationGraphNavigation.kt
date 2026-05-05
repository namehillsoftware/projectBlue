package com.lasthopesoftware.bluewater.client.browsing.navigation

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.ResponsiveState
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
import dev.olshevski.navigation.reimagined.replaceAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ResponsiveDestinationGraphNavigation(
    private val inner: NavigateApplication,
	private val draggableState: AnchoredDraggableState<ResponsiveState>,
    private val navController: NavController<Destination>,
	private val libraryNavController: NavController<BrowserLibraryDestination>,
    private val coroutineScope: CoroutineScope,
    private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) : NavigateApplication by inner {

	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		navigateToBrowserDestination(FilePropertySearchScreen(libraryId))
	}.toPromise()

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> = coroutineScope.launch {
		navigateToBrowserDestination(FilePropertySearchScreen(libraryId, filePropertyFilter))
	}.toPromise()

	override fun search(libraryId: LibraryId, searchQuery: String): Promise<Unit> = coroutineScope.launch {
		navigateToBrowserDestination(SearchScreen(libraryId, searchQuery))
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
		navController.navigate(ConnectionSettingsScreen(libraryId))
	}.toPromise()

	override fun viewActiveDownloads(libraryId: LibraryId) = coroutineScope.launch {
		navigateToBrowserDestination(DownloadsScreen(libraryId))
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navigateToBrowserDestination(LibraryScreen(libraryId))
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		navigateToBrowserDestination(ItemScreen(libraryId, item))
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

	override fun viewNowPlaying(libraryId: LibraryId) = coroutineScope.launch {
		ensureBrowserIsOnStack(libraryId)

		if (draggableState.currentValue < ResponsiveState.NowPlaying)
			draggableState.animateTo(ResponsiveState.NowPlaying)
	}.toPromise()

	override fun navigateUp() = coroutineScope.async {
		if (navController.backstack.entries.lastOrNull()?.destination is BrowserLibraryDestination && draggableState.currentValue > ResponsiveState.Browser) {
			draggableState.animateTo(ResponsiveState.Browser)
			true
		} else {
			(libraryNavController.pop() && libraryNavController.backstack.entries.any()) ||
				(navController.pop() && navController.backstack.entries.any()) ||
				inner.navigateUp().suspend()
		}
	}.toPromise()

	override fun backOut() = coroutineScope.async {
		itemListMenuBackPressedHandler.hideAllMenus() || navigateUp().suspend()
	}.toPromise()

	private suspend fun navigateToBrowserDestination(destination: BrowserLibraryDestination) {
		val libraryId = destination.libraryId

		bringBrowserIntoView(libraryId)

		libraryNavController.navigate(destination)
	}

	private suspend fun bringBrowserIntoView(libraryId: LibraryId) {
		ensureBrowserIsOnStack(libraryId)
		draggableState.animateTo(ResponsiveState.Browser)
	}

	private fun ensureBrowserIsOnStack(libraryId: LibraryId) {
		if (!navController.popUpTo { it is LibraryScreen && it.libraryId == libraryId }) {
			navController.popUpTo { it is ApplicationSettingsScreen }
			navController.navigate(LibraryScreen(libraryId))
			libraryNavController.replaceAll(LibraryScreen(libraryId))
		}
	}
}
