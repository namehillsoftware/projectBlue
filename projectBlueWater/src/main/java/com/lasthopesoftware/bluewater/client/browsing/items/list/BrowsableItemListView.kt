import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.promises.extensions.suspend
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
	connectionStatusViewModel: ConnectionStatusViewModel,
): @Composable (LibraryId, Item?) -> Unit {
	var isConnectionLost by remember { mutableStateOf(false) }
	var reinitializeConnection by remember { mutableStateOf(false) }

	if (isConnectionLost) {
		ConnectionLostView(
			onCancel = { applicationNavigation.viewApplicationSettings() },
			onRetry = {
				reinitializeConnection = true
			}
		)
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
		if (reinitializeConnection) {
			LaunchedEffect(key1 = Unit) {
				isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
				reinitializeConnection = false
			}
		}

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
