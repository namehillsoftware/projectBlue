import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeadlineViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.selected.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise

object ItemBrowsingArguments {
	const val libraryIdArgument = "libraryId"
	const val keyArgument = "key"
	const val titleArgument = "title"
	const val playlistIdArgument = "playlistId"
}

@Composable
fun NavBackStackEntry.BrowsableItemListView(
	connectionViewModel: ConnectionStatusViewModel,
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuViewModel: ItemListMenuViewModel,
	trackHeadlineViewModelProvider: TrackHeadlineViewModelProvider,
	applicationNavigation: NavigateApplication,
) {
	val isCheckingConnection by connectionViewModel.isGettingConnection.collectAsState()
	if (!isCheckingConnection) {
		ItemListView(
			itemListViewModel,
			fileListViewModel,
			nowPlayingViewModel,
			itemListMenuViewModel,
			trackHeadlineViewModelProvider,
			applicationNavigation,
		)
	} else {
		ConnectionUpdatesView(connectionViewModel)
	}

	val libraryId = arguments?.getInt(ItemBrowsingArguments.libraryIdArgument)?.let(::LibraryId) ?: return
	val playlistId = arguments?.getInt(ItemBrowsingArguments.playlistIdArgument)
	val item = if (playlistId != null && playlistId > -1) {
		Item(
			arguments?.getInt(ItemBrowsingArguments.keyArgument) ?: return,
			arguments?.getString(ItemBrowsingArguments.titleArgument),
			PlaylistId(playlistId),
		)
	} else {
		Item(
			arguments?.getInt(ItemBrowsingArguments.keyArgument) ?: return,
			arguments?.getString(ItemBrowsingArguments.titleArgument)
		)
	}

	LaunchedEffect(item) {
		connectionViewModel.ensureConnectionIsWorking(libraryId).suspend()
		Promise.whenAll(
			itemListViewModel.loadItem(libraryId, item),
			fileListViewModel.loadItem(item)
		).suspend()
	}
}
