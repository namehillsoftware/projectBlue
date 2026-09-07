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

	override fun launchSearch(libraryId: LibraryId) =
		navigateToBrowserDestination(FilePropertySearchScreen(libraryId))

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> =
		navigateToBrowserDestination(FilePropertySearchScreen(libraryId, filePropertyFilter))

	override fun search(libraryId: LibraryId, searchQuery: String): Promise<Unit> =
		navigateToBrowserDestination(SearchScreen(libraryId, searchQuery))

	override fun viewAllDownloads(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }
		return bringBrowserIntoView().then { navController.navigate(AllDownloadsScreen) }
	}

	override fun viewApplicationSettings(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }
		return bringBrowserIntoView()
	}

	override fun viewHiddenSettings(): Promise<Unit> =
		bringBrowserIntoView().then { navController.navigate(HiddenSettingsScreen) }

	override fun viewNewServerSettings(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }
		return bringBrowserIntoView().then { navController.navigate(NewConnectionSettingsScreen) }
	}

	override fun viewServerSettings(libraryId: LibraryId): Promise<Unit> =
		bringBrowserIntoView().then { navController.navigate(ConnectionSettingsScreen(libraryId)) }

	override fun viewActiveDownloads(libraryId: LibraryId): Promise<Unit> =
		bringBrowserIntoView().then { navController.navigate(DownloadsScreen(libraryId)) }

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> =
		if (libraryNavController.peek()?.destination?.libraryId != libraryId) navigateToBrowserDestination(LibraryScreen(libraryId))
		else Unit.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = navigateToBrowserDestination(ItemScreen(libraryId, item))

	override fun viewFileDetails(libraryId: LibraryId, searchQuery: String, positionedFile: PositionedFile): Promise<Unit> {
		navController.navigate(SearchedFileDetailsScreen(libraryId, searchQuery, positionedFile))
		return Unit.toPromise()
	}

	override fun viewFileDetails(libraryId: LibraryId, item: IItem?, positionedFile: PositionedFile): Promise<Unit> {
		navController.navigate(BrowsedFileDetailsScreen(libraryId, item, positionedFile))
		return Unit.toPromise()
	}

	override fun viewNowPlayingFileDetails(libraryId: LibraryId, positionedFile: PositionedFile): Promise<Unit> {
		navController.navigate(FileDetailsFromNowPlayingScreen(libraryId, positionedFile))
		return Unit.toPromise()
	}

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> {
		ensureBrowserIsOnStack(libraryId)
		return coroutineScope.launch {
			if (draggableState.currentValue < ResponsiveState.NowPlaying)
				draggableState.animateTo(ResponsiveState.NowPlaying)
		}.toPromise()
	}

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

	private fun navigateToBrowserDestination(destination: BrowserLibraryDestination): Promise<Unit> {
		val libraryId = destination.libraryId

		ensureBrowserIsOnStack(libraryId)
		return bringBrowserIntoView()
			.then {
				if (libraryNavController.peek()?.destination != destination)
					libraryNavController.navigate(destination)
			}
	}

	private fun bringBrowserIntoView(): Promise<Unit> {
		return with (draggableState) {
			if (currentValue > ResponsiveState.Browser) {
				coroutineScope.launch {
					animateTo(
						if (anchors.hasPositionFor(ResponsiveState.Split)) ResponsiveState.Split
						else ResponsiveState.Browser
					)
				}.toPromise()
			} else Unit.toPromise()
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
