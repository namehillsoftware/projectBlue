import androidx.compose.runtime.*
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.*
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException

@Composable
fun browsableItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	reusablePlaylistFileItemViewModelProvider: ReusablePlaylistFileItemViewModelProvider,
	childItemViewModelProvider: PooledCloseablesViewModel<ReusableChildItemViewModel>,
	applicationNavigation: NavigateApplication,
	playbackLibraryItems: PlaybackLibraryItems,
	playbackServiceController: ControlPlaybackService,
): @Composable (LibraryId, Item?) -> Unit {
	var isConnectionLost by remember { mutableStateOf(false) }

	if (isConnectionLost) {
		ConnectionLostView(applicationNavigation) {
			isConnectionLost = false
		}
	} else {
		ItemListView(
			itemListViewModel,
			fileListViewModel,
			nowPlayingViewModel,
			itemListMenuBackPressedHandler,
			reusablePlaylistFileItemViewModelProvider,
			childItemViewModelProvider,
			applicationNavigation,
			playbackLibraryItems,
			playbackServiceController,
		)
	}

	return { libraryId: LibraryId, item: Item? ->
		if (!isConnectionLost) {
			LaunchedEffect(item) {
				try {
					Promise.whenAll(
						itemListViewModel.loadItem(libraryId, item),
						fileListViewModel.loadItem(libraryId, item),
					).suspend()
				} catch (e: IOException) {
					if (ConnectionLostExceptionFilter.isConnectionLostException(e))
						isConnectionLost = true
					else
						applicationNavigation.backOut().suspend()
				} catch (e: Exception) {
					applicationNavigation.backOut().suspend()
				}
			}
		}
	}
}
