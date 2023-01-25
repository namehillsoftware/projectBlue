package com.lasthopesoftware.bluewater.client.browsing.items

import BrowsableItemListView
import ItemBrowsingArguments.keyArgument
import ItemBrowsingArguments.libraryIdArgument
import ItemBrowsingArguments.playlistIdArgument
import ItemBrowsingArguments.titleArgument
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesView
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeadlineViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.RateControlledFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.SelectedLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.selected.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.resources.strings.StringResources

private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<ItemBrowserActivity>()) }

private val libraryIdProperty by lazy { magicPropertyBuilder.buildProperty(libraryIdArgument) }
private val keyProperty by lazy { magicPropertyBuilder.buildProperty(keyArgument) }
private val itemTitleProperty by lazy { magicPropertyBuilder.buildProperty(titleArgument) }
private val playlistIdProperty by lazy { magicPropertyBuilder.buildProperty(playlistIdArgument) }

fun Context.startItemBrowserActivity(libraryId: LibraryId, item: IItem) {
	if (item is Item) startItemBrowserActivity(libraryId, item)
	else startActivity(getItemBrowserIntent(this, libraryId, item))
}

fun Context.startItemBrowserActivity(libraryId: LibraryId, item: Item) {
	val fileListIntent = getItemBrowserIntent(this, libraryId, item).apply {
		item.playlistId?.also { putExtra(playlistIdProperty, it.id) }
	}
	startActivity(fileListIntent)
}

private fun getItemBrowserIntent(context: Context, libraryId: LibraryId, item: IItem) = Intent(context, cls<ItemBrowserActivity>()).apply {
	putExtra(keyProperty, item.key)
	putExtra(itemTitleProperty, item.value)
	putExtra(libraryIdProperty, libraryId.id)
}

class ItemBrowserActivity : AppCompatActivity() {

	private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(1) }

	private val browserLibraryIdProvider by lazy { getCachedSelectedLibraryIdProvider() }

	private val messageBus by lazy { getApplicationMessageBus() }

	private val menuMessageBus by buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	private val itemListMenuViewModel by buildViewModelLazily { ItemListMenuViewModel(menuMessageBus) }

	private val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(libraryConnectionProvider) }

	private val itemListProvider by lazy {
		ItemStringListProvider(
			FileListParameters,
			libraryFileStringListProvider
		)
	}

	private val fileProvider by lazy {
		ItemFileProvider(
			ItemStringListProvider(
				FileListParameters,
				libraryFileStringListProvider
			)
		)
	}

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			RateControlledFilePropertiesProvider(
				FilePropertiesProvider(
					libraryConnectionProvider,
					revisionProvider,
					FilePropertyCache,
				),
				rateLimiter,
			),
		)
	}

	private val scopedFilePropertiesProvider by lazy {
		SelectedLibraryFilePropertiesProvider(
			browserLibraryIdProvider,
			libraryFilePropertiesProvider,
		)
	}

	private val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			browserLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider),
		)
	}

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val connectionAuthenticationChecker by lazy {
		ConnectionAuthenticationChecker(libraryConnectionProvider)
	}

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val filePropertiesStorage by lazy {
		FilePropertyStorage(
			libraryConnectionProvider,
			connectionAuthenticationChecker,
			revisionProvider,
			FilePropertyCache,
			messageBus
		)
	}

	private val playbackServiceController by lazy { PlaybackServiceController(this) }

	private val nowPlayingFilePropertiesViewModel by buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			messageBus,
			LiveNowPlayingLookup.getInstance(),
			libraryFilePropertiesProvider,
			UrlKeyProvider(libraryConnectionProvider),
			filePropertiesStorage,
			connectionAuthenticationChecker,
			playbackServiceController,
			ConnectionPoller(this),
			StringResources(this),
		).apply { initializeViewModel() }
	}

	private val storedItemAccess by lazy {
		StateChangeBroadcastingStoredItemAccess(StoredItemAccess(this), messageBus)
	}

	private val stringResources by lazy { StringResources(this) }

	private val libraryFilesProvider by lazy { LibraryFileProvider(LibraryFileStringListProvider(libraryConnectionProvider))  }

	private val activityApplicationNavigation by lazy { ActivityApplicationNavigation(this) }

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val libraryIdInt = savedInstanceState?.getInt(libraryIdProperty, -1) ?: intent.getIntExtra(libraryIdProperty, -1)
		val libraryId = LibraryId(libraryIdInt)
		val playlistId = savedInstanceState?.getInt(playlistIdProperty, -1) ?: intent.getIntExtra(playlistIdProperty, -1)
		val item = if (playlistId > -1) {
			Item(
				savedInstanceState?.getInt(keyProperty) ?: intent.getIntExtra(keyProperty, 1),
				savedInstanceState?.getString(itemTitleProperty) ?: intent.getStringExtra(itemTitleProperty),
				PlaylistId(playlistId),
			)
		} else {
			Item(
				savedInstanceState?.getInt(keyProperty) ?: intent.getIntExtra(keyProperty, 1),
				savedInstanceState?.getString(itemTitleProperty) ?: intent.getStringExtra(itemTitleProperty),
			)
		}

		setContent {
			ProjectBlueTheme {
				ItemBrowserView(
					nowPlayingFilePropertiesViewModel,
					browserLibraryIdProvider,
					itemProvider,
					itemListProvider,
					messageBus,
					storedItemAccess,
					playbackServiceController,
					fileProvider,
					menuMessageBus,
					itemListMenuViewModel,
					scopedFilePropertiesProvider,
					scopedUrlKeyProvider,
					stringResources,
					libraryFilesProvider,
					activityApplicationNavigation,
					libraryConnectionProvider,
					libraryId,
					item,
				)
			}
		}
	}

	override fun onBackPressed() {
		if (!itemListMenuViewModel.hideAllMenus()) super.onBackPressed()
	}
}

private class GraphNavigation(private val navController: NavHostController, private val inner: NavigateApplication) : NavigateApplication by inner {
	object Search {
		const val route = "search"
	}

	object BrowseToItem {
		const val route = "library/{$libraryIdArgument}/item/{$keyArgument}?$titleArgument={$titleArgument}&$playlistIdArgument={$playlistIdArgument}"

		fun buildPath(libraryId: LibraryId, item: IItem): String {
			var path = "library/${libraryId.id}/item/${item.key}?$titleArgument=${item.value}"
			if (item is Item) {
				val playlistId = item.playlistId
				if (playlistId != null)
					path += "&$playlistIdArgument=${playlistId.id}"
			}

			return path
		}
	}

	override fun launchSearch() {
		navController.navigate(Search.route) {
			launchSingleTop = true
		}
	}

	override fun viewItem(libraryId: LibraryId, item: IItem) {
		navController.navigate(BrowseToItem.buildPath(libraryId, item))
	}

	override fun backOut(): Boolean = !navController.navigateUp() && inner.backOut()
}

@Composable
private fun ItemBrowserView(
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	browserLibraryIdProvider: CachedSelectedLibraryIdProvider,
	itemProvider: CachedItemProvider,
	itemListProvider: ItemStringListProvider,
	messageBus: ApplicationMessageBus,
	storedItemAccess: StateChangeBroadcastingStoredItemAccess,
	playbackServiceController: PlaybackServiceController,
	itemFileProvider: ItemFileProvider,
	menuMessageBus: ViewModelMessageBus<ItemListMenuMessage>,
	itemListMenuViewModel: ItemListMenuViewModel,
	scopedFilePropertiesProvider: SelectedLibraryFilePropertiesProvider,
	scopedUrlKeyProvider: SelectedLibraryUrlKeyProvider,
	stringResources: StringResources,
	libraryFilesProvider: LibraryFileProvider,
	applicationNavigation: NavigateApplication,
	libraryConnectionProvider: ConnectionSessionManager,
	startingLibraryId: LibraryId? = null,
	startingItem: IItem? = null,
) {
	val activity = LocalContext.current as? ComponentActivity ?: return

	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(MaterialTheme.colors.surface)

	val navController = rememberNavController()

	val graphNavigation = GraphNavigation(navController, applicationNavigation)

	Scaffold(bottomBar = {
		BottomAppBar(
			backgroundColor = MaterialTheme.colors.secondary,
			contentPadding = PaddingValues(0.dp),
			modifier = Modifier
				.clickable(onClick = graphNavigation::viewNowPlaying)
		) {
			Column {
				Row(
					modifier = Modifier
						.weight(1f)
						.padding(end = 16.dp)
				) {
					Box(
						modifier = Modifier
							.align(Alignment.CenterVertically)
							.fillMaxHeight()
							.clickable(onClick = graphNavigation::launchSearch),
					) {
						Icon(
							Icons.Default.Search,
							contentDescription = stringResource(id = R.string.lbl_search),
							tint = MaterialTheme.colors.onSecondary,
							modifier = Modifier
								.align(Alignment.Center)
								.padding(start = 16.dp, end = 16.dp)
						)
					}

					Column(
						modifier = Modifier
							.weight(1f)
							.align(Alignment.CenterVertically),
					) {
						val songTitle by nowPlayingViewModel.title.collectAsState()

						ProvideTextStyle(MaterialTheme.typography.subtitle1) {
							Text(
								text = songTitle
									?: stringResource(id = R.string.lbl_loading),
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
								fontWeight = FontWeight.Medium
							)
						}

						val songArtist by nowPlayingViewModel.artist.collectAsState()
						ProvideTextStyle(MaterialTheme.typography.body2) {
							Text(
								text = songArtist
									?: stringResource(id = R.string.lbl_loading),
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
							)
						}
					}

					val isPlaying by nowPlayingViewModel.isPlaying.collectAsState()
					Image(
						painter = painterResource(id = if (!isPlaying) R.drawable.av_play_white else R.drawable.av_pause_white),
						contentDescription = stringResource(id = R.string.btn_play),
						modifier = Modifier
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = {
									if (!isPlaying) PlaybackService.play(activity)
									else PlaybackService.pause(activity)

									nowPlayingViewModel.togglePlaying(!isPlaying)
								}
							)
							.padding(start = 8.dp, end = 8.dp)
							.align(Alignment.CenterVertically)
							.size(24.dp),
					)

					Icon(
						Icons.Default.ArrowForward,
						contentDescription = "",
						tint = MaterialTheme.colors.onSecondary,
						modifier = Modifier
							.padding(start = 8.dp, end = 8.dp)
							.align(Alignment.CenterVertically)
					)
				}

				val filePosition by nowPlayingViewModel.filePosition.collectAsState()
				val fileDuration by nowPlayingViewModel.fileDuration.collectAsState()
				val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }
				LinearProgressIndicator(
					progress = fileProgress,
					color = MaterialTheme.colors.primary,
					backgroundColor = MaterialTheme.colors.onPrimary.copy(alpha = .6f),
					modifier = Modifier
						.fillMaxWidth()
						.padding(0.dp)
				)
			}
		}
	}) { paddingValues ->
		NavHost(
			navController,
			modifier = Modifier
				.padding(paddingValues)
				.fillMaxSize(),
			startDestination = GraphNavigation.BrowseToItem.route,
		) {
			composable(
				GraphNavigation.BrowseToItem.route,
				arguments = listOf(
					navArgument(libraryIdArgument) {
						type = NavType.IntType
						defaultValue = startingLibraryId?.id ?: -1
					},
					navArgument(keyArgument) {
						type = NavType.IntType
						defaultValue = startingItem?.key ?: -1
					},
					navArgument(titleArgument) {
						type = NavType.StringType
						nullable = true
						defaultValue = startingItem?.value
					},
					navArgument(playlistIdArgument) {
						type = NavType.IntType
						defaultValue = startingItem.let { it as? Item }?.playlistId?.id ?: -1
					},
				)
			) { entry ->
				entry.BrowsableItemListView(
					entry.viewModelStore.buildViewModel {
						ConnectionStatusViewModel(
							stringResources,
							libraryConnectionProvider,
						)
					},
					entry.viewModelStore.buildViewModel {
						ItemListViewModel(
							itemProvider,
							messageBus,
							storedItemAccess,
							itemListProvider,
							playbackServiceController,
							graphNavigation,
							menuMessageBus,
						)
					},
					entry.viewModelStore.buildViewModel {
						FileListViewModel(
							browserLibraryIdProvider,
							itemFileProvider,
							storedItemAccess,
							playbackServiceController,
						)
					},
					nowPlayingViewModel,
					itemListMenuViewModel,
					trackHeadlineViewModelProvider = entry.viewModelStore.buildViewModel {
						TrackHeadlineViewModelProvider(
							scopedFilePropertiesProvider,
							scopedUrlKeyProvider,
							stringResources,
							playbackServiceController,
							graphNavigation,
							menuMessageBus,
							messageBus,
						)
					},
					graphNavigation,
				)
			}

			composable(GraphNavigation.Search.route) { entry ->
				SearchFilesView(
					searchFilesViewModel = entry.viewModelStore.buildViewModel {
						SearchFilesViewModel(
							browserLibraryIdProvider,
							libraryFilesProvider,
							playbackServiceController,
						)
					},
					nowPlayingViewModel = nowPlayingViewModel,
					trackHeadlineViewModelProvider = entry.viewModelStore.buildViewModel {
						TrackHeadlineViewModelProvider(
							scopedFilePropertiesProvider,
							scopedUrlKeyProvider,
							stringResources,
							playbackServiceController,
							graphNavigation,
							menuMessageBus,
							messageBus,
						)
					},
					itemListMenuViewModel = itemListMenuViewModel,
					onBack = graphNavigation::backOut,
				)
			}
		}
	}
}
