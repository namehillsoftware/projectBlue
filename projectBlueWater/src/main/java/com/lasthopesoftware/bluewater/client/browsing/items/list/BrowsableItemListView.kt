import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise

object ItemBrowsingArguments {
	const val libraryIdArgument = "libraryId"
}

@Composable
fun browsableItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	reusablePlaylistFileItemViewModelProvider: ReusablePlaylistFileItemViewModelProvider,
	applicationNavigation: NavigateApplication,
	playbackLibraryItems: PlaybackLibraryItems,
	playbackServiceController: ControlPlaybackService,
): @Composable (LibraryId, Item?) -> Unit {
	ItemListView(
		itemListViewModel,
		fileListViewModel,
		nowPlayingViewModel,
		itemListMenuBackPressedHandler,
		reusablePlaylistFileItemViewModelProvider,
		applicationNavigation,
		playbackLibraryItems,
		playbackServiceController,
	)

	return { libraryId: LibraryId, item: Item? ->
		LaunchedEffect(item) {
			try {
				Promise.whenAll(
					itemListViewModel.loadItem(libraryId, item),
					fileListViewModel.loadItem(libraryId, item),
				).suspend()
			} catch (e: Exception) {
				applicationNavigation.backOut().suspend()
			}
		}
	}
}
