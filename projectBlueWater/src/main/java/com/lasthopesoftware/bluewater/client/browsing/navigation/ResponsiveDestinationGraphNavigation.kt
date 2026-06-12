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
import com.lasthopesoftware.navigation.isNotEmpty
import com.lasthopesoftware.navigation.peek
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
		if (libraryNavController.peek()?.destination?.libraryId != libraryId)
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
		if (navController.peek()?.destination is BrowserLibraryDestination) {
			with (draggableState) {
				if (currentValue > ResponsiveState.Browser) {
					// Navigate Up will only return to the Browser state
					animateTo(
						if (anchors.hasPositionFor(ResponsiveState.Split)) ResponsiveState.Split
						else ResponsiveState.Browser
					)
					return@async true
				}
			}

			if (libraryNavController.pop() && libraryNavController.isNotEmpty()) {
				return@async true
			}
		}

		(navController.pop() && navController.isNotEmpty()) || inner.navigateUp().suspend()
	}.toPromise()

	override fun backOut() = coroutineScope.async {
		if (itemListMenuBackPressedHandler.hideAllMenus()) return@async true

		if (navController.peek()?.destination is BrowserLibraryDestination) {
			if (draggableState.currentValue > ResponsiveState.Split) {
				// Back-out will reverse through all prior states
				val animatedToPreviousState = ResponsiveState
					.entries
					.asReversed()
					// only consider values less than current state
					.dropWhile { it != draggableState.currentValue }
					.firstOrNull {
						it != draggableState.currentValue && draggableState.anchors.hasPositionFor(it)
					}
					?.let {
						draggableState.animateTo(it)
					}
				if (animatedToPreviousState != null) return@async true
			}
		}

		navigateUp().suspend()
	}.toPromise()

	private suspend fun navigateToBrowserDestination(destination: BrowserLibraryDestination) {
		val libraryId = destination.libraryId

		bringBrowserIntoView(libraryId)

		if (libraryNavController.peek()?.destination != destination)
			libraryNavController.navigate(destination)
	}

	private suspend fun bringBrowserIntoView(libraryId: LibraryId) {
		ensureBrowserIsOnStack(libraryId)
//		draggableState.animateTo(ResponsiveState.Browser)
		with (draggableState) {
			animateTo(
				if (anchors.hasPositionFor(ResponsiveState.Split)) ResponsiveState.Split
				else ResponsiveState.Browser
			)
		}
	}

	private fun ensureBrowserIsOnStack(libraryId: LibraryId) {
		if (!navController.popUpTo { it is LibraryScreen && it.libraryId == libraryId }) {
			navController.popUpTo { it is ApplicationSettingsScreen }

			val destination = LibraryScreen(libraryId)
			navController.navigate(destination)
			libraryNavController.replaceAll(destination)
		}
	}
}
