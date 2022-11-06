package com.lasthopesoftware.bluewater.client.browsing.items

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
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

private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ItemBrowserActivity::class.java) }

private const val keyArgument = "key"
private const val titleArgument = "title"
private const val playlistIdArgument = "playlistId"

private val keyProperty by lazy { magicPropertyBuilder.buildProperty(keyArgument) }
private val itemTitleProperty by lazy { magicPropertyBuilder.buildProperty(titleArgument) }
private val playlistIdProperty by lazy { magicPropertyBuilder.buildProperty(playlistIdArgument) }
private val searchAction by lazy { magicPropertyBuilder.buildProperty("search") }

fun Context.startItemSearchActivity() {
	startActivity(Intent(this, cls<ItemBrowserActivity>()).apply { action = searchAction })
}

fun Context.startItemBrowserActivity(item: IItem) {
	if (item is Item) startItemBrowserActivity(item)
	else startActivity(getItemBrowserIntent(this, item))
}

fun Context.startItemBrowserActivity(item: Item) {
	val fileListIntent = getItemBrowserIntent(this, item).apply {
		item.playlistId?.also { putExtra(playlistIdProperty, it.id) }
	}
	startActivity(fileListIntent)
}

private fun getItemBrowserIntent(context: Context, item: IItem) = Intent(context, cls<ItemBrowserActivity>()).apply {
	putExtra(keyProperty, item.key)
	putExtra(itemTitleProperty, item.value)
}

class ItemBrowserActivity : AppCompatActivity() {

	private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(2) }

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

	private val fileDetailsLauncher by lazy { ActivityApplicationNavigation(this) }

	private val libraryFilesProvider by lazy { LibraryFileProvider(LibraryFileStringListProvider(libraryConnectionProvider))  }

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val isSearch = intent.action == searchAction

		val item = if (isSearch) null
		else {
			val playlistId = savedInstanceState?.getInt(playlistIdProperty, -1) ?: intent.getIntExtra(playlistIdProperty, -1)
			if (playlistId > -1) {
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
					fileDetailsLauncher,
					libraryFilesProvider,
					item,
					isSearch
				)
			}
		}
	}

	override fun onBackPressed() {
		if (!itemListMenuViewModel.hideAllMenus()) super.onBackPressed()
	}
}

private object NavigationGraphRoutes {
	const val search = "search"

	object BrowseToItem {
		const val route = "browse/{$keyArgument}?$titleArgument={$titleArgument}&$playlistIdArgument={$playlistIdArgument}"

		fun buildPath(item: IItem): String {
			var path = "browse/${item.key}?$titleArgument=${item.value}"
			if (item is Item) {
				val playlistId = item.playlistId
				if (playlistId != null)
					path += "&$playlistIdArgument=${playlistId.id}"
			}

			return path
		}
	}
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
	applicationNavigation: ActivityApplicationNavigation,
	libraryFilesProvider: LibraryFileProvider,
	startingItem: IItem? = null,
	isSearch: Boolean = false,
) {
	val activity = LocalContext.current as? Activity ?: return

	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(MaterialTheme.colors.surface)

	val navController = rememberNavController()

	val graphNavigator = object : NavigateApplication by applicationNavigation {
		override fun launchSearch() {
			navController.navigate(NavigationGraphRoutes.search)
		}

		override fun viewItem(item: IItem) {
			navController.navigate(NavigationGraphRoutes.BrowseToItem.buildPath(item))
		}
	}

	Scaffold(bottomBar = {
		BottomAppBar(
			backgroundColor = MaterialTheme.colors.secondary,
			contentPadding = PaddingValues(0.dp),
			modifier = Modifier
				.clickable { applicationNavigation.viewNowPlaying() }
		) {
			Column {
				Row(
					modifier = Modifier
						.weight(1f)
						.padding(start = 16.dp, end = 16.dp)
				) {
					Icon(
						Icons.Default.Search,
						contentDescription = stringResource(id = R.string.lbl_search),
						tint = MaterialTheme.colors.onSecondary,
						modifier = Modifier
							.padding(start = 8.dp, end = 8.dp)
							.align(Alignment.CenterVertically)
							.clickable(onClick = graphNavigator::launchSearch),
					)

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
			startDestination = if (!isSearch) NavigationGraphRoutes.BrowseToItem.route else NavigationGraphRoutes.search,
		) {
			composable(
				NavigationGraphRoutes.BrowseToItem.route,
				arguments = listOf(
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
				val playlistId = entry.arguments?.getInt(playlistIdArgument)
				val item = if (playlistId != null && playlistId > -1) {
					Item(
						entry.arguments?.getInt(keyArgument) ?: return@composable,
						entry.arguments?.getString(titleArgument),
						PlaylistId(playlistId),
					)
				} else {
					Item(
						entry.arguments?.getInt(keyArgument) ?: return@composable,
						entry.arguments?.getString(titleArgument)
					)
				}

				val itemListViewModel = entry.viewModelStore.buildViewModel {
					ItemListViewModel(
						browserLibraryIdProvider,
						itemProvider,
						messageBus,
						storedItemAccess,
						itemListProvider,
						playbackServiceController,
						graphNavigator,
						menuMessageBus,
					)
				}

				val fileListViewModel = entry.viewModelStore.buildViewModel {
					FileListViewModel(
						browserLibraryIdProvider,
						itemFileProvider,
						storedItemAccess,
						playbackServiceController,
					)
				}

				ItemListView(
					itemListViewModel = itemListViewModel,
					fileListViewModel = fileListViewModel,
					nowPlayingViewModel = nowPlayingViewModel,
					itemListMenuViewModel = itemListMenuViewModel,
					trackHeadlineViewModelProvider = entry.viewModelStore.buildViewModel {
						TrackHeadlineViewModelProvider(
							scopedFilePropertiesProvider,
							scopedUrlKeyProvider,
							stringResources,
							playbackServiceController,
							graphNavigator,
							menuMessageBus,
							messageBus,
						)
					}
				) {
					if (!navController.navigateUp())
						activity.finish()
				}

				LaunchedEffect(item) {
					itemListViewModel.loadItem(item)
					fileListViewModel.loadItem(item)
				}
			}

			composable(NavigationGraphRoutes.search) { entry ->
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
							applicationNavigation,
							menuMessageBus,
							messageBus,
						)
					},
					itemListMenuViewModel = itemListMenuViewModel,
				)
			}
		}
	}
}
