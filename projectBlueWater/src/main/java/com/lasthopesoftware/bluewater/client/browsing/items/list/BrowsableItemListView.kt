import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.ViewDependencies
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
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException

@Composable
fun LoadedItemListView(viewModelDependencies: ViewDependencies, libraryId: LibraryId, item: Item?) {
	with (viewModelDependencies) {
		LoadedItemListView(
			libraryId,
			item,
			itemListViewModel = itemListViewModel,
			fileListViewModel = fileListViewModel,
			nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
			itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
			reusablePlaylistFileItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
			childItemViewModelProvider = reusableChildItemViewModelProvider,
			applicationNavigation = applicationNavigation,
			playbackLibraryItems = playbackLibraryItems,
			playbackServiceController = playbackServiceController,
			connectionStatusViewModel = connectionStatusViewModel,
		)
	}
}

@Composable
private fun LoadedItemListView(
	libraryId: LibraryId,
	item: Item?,
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
) {
	var isConnectionLost by remember { mutableStateOf(false) }
	var initializeConnection by remember { mutableStateOf(true) }

	if (isConnectionLost) {
		ConnectionLostView(
			onCancel = { applicationNavigation.viewApplicationSettings() },
			onRetry = {
				initializeConnection = true
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

	ViewModelInitAction {
		if (initializeConnection) {
			LaunchedEffect(key1 = Unit) {
				isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
				initializeConnection = false
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
