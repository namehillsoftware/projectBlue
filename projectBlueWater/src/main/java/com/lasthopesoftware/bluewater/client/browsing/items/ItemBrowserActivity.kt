@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lasthopesoftware.bluewater.client.browsing.items

import ItemBrowsingArguments.keyArgument
import ItemBrowsingArguments.libraryIdArgument
import ItemBrowsingArguments.playlistIdArgument
import ItemBrowsingArguments.titleArgument
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import browsableItemListView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.*
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.RateControlledFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.SelectedLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.access.*
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializationErrorController
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializationProxy
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.ViewModelCloseableManager
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<ItemBrowserActivity>()) }

val libraryIdProperty by lazy { magicPropertyBuilder.buildProperty(libraryIdArgument) }
val keyProperty by lazy { magicPropertyBuilder.buildProperty(keyArgument) }
val itemTitleProperty by lazy { magicPropertyBuilder.buildProperty(titleArgument) }
val playlistIdProperty by lazy { magicPropertyBuilder.buildProperty(playlistIdArgument) }
val downloadsRoute by lazy { magicPropertyBuilder.buildProperty(GraphNavigation.Downloads.nestedRoute) }

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

private fun getItemBrowserIntent(context: Context, libraryId: LibraryId, item: IItem) =
	getItemBrowserIntent(context, libraryId).apply {
		putExtra(keyProperty, item.key)
		putExtra(itemTitleProperty, item.value)
	}

private fun getItemBrowserIntent(context: Context, libraryId: LibraryId) =
	context.getIntent<ItemBrowserActivity>().apply {
		putExtra(libraryIdProperty, libraryId.id)
	}

class ItemBrowserActivity :
	AppCompatActivity(),
	ItemBrowserViewDependencies,
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions
{

	private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(1) }

	private val viewModelScope by buildViewModelLazily { ViewModelCloseableManager() }

	override val browserLibraryIdProvider by lazy { getCachedSelectedLibraryIdProvider() }

	override val messageBus by lazy { getApplicationMessageBus().getScopedMessageBus().also(viewModelScope::manage) }

	override val menuMessageBus by buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	override val itemListMenuBackPressedHandler by lazyScoped { ItemListMenuBackPressedHandler(menuMessageBus) }

	override val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(libraryConnectionProvider) }

	override val itemListProvider by lazy {
		ItemStringListProvider(
			FileListParameters,
			libraryFileStringListProvider
		)
	}

	override val itemFileProvider by lazy {
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

	override val scopedFilePropertiesProvider by lazy {
		SelectedLibraryFilePropertiesProvider(
			browserLibraryIdProvider,
			libraryFilePropertiesProvider,
		)
	}

	override val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			browserLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider),
		)
	}

	override val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

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

	override val playbackServiceController by lazy { PlaybackServiceController(this) }

	override val nowPlayingFilePropertiesViewModel by buildViewModelLazily {
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

	override val storedItemAccess by lazy {
		StateChangeBroadcastingStoredItemAccess(StoredItemAccess(this), messageBus)
	}

	override val storedFileAccess by lazy { StoredFileAccess(this) }

	override val stringResources by lazy { StringResources(this) }

	override val libraryFilesProvider by lazy {
		LibraryFileProvider(
			LibraryFileStringListProvider(
				libraryConnectionProvider
			)
		)
	}

	override val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			this,
			IntentBuilder(this),
			BrowserLibrarySelection(
				getApplicationSettingsRepository(),
				messageBus,
				libraryProvider,
			),
			browserLibraryIdProvider,
		)
	}

	override val syncScheduler by lazy { SyncScheduler(this) }

	private val libraryRepository by lazy { LibraryRepository(this) }

	override val libraryProvider: ILibraryProvider
		get() = libraryRepository

	override val libraryStorage by lazy {
		ObservableConnectionSettingsLibraryStorage(
			libraryRepository,
			ConnectionSettingsLookup(libraryProvider),
			messageBus
		)
	}

	override val libraryRemoval by lazy {
		LibraryRemoval(
			storedItemAccess,
			libraryRepository,
			browserLibraryIdProvider,
			libraryRepository,
			BrowserLibrarySelection(getApplicationSettingsRepository(), messageBus, libraryProvider),
		)
	}

	override val readPermissionsRequirements by lazy { ApplicationReadPermissionsRequirementsProvider(this) }
	override val writePermissionsRequirements by lazy { ApplicationWritePermissionsRequirementsProvider(this) }
	override val permissionsManager = this

	private val permissionsRequests = ConcurrentHashMap<Int, Messenger<Map<String, Boolean>>>()

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val libraryIdInt =
			savedInstanceState?.getInt(libraryIdProperty, -1) ?: intent.getIntExtra(libraryIdProperty, -1)
		val libraryId = LibraryId(libraryIdInt)
		val playlistId =
			savedInstanceState?.getInt(playlistIdProperty, -1) ?: intent.getIntExtra(playlistIdProperty, -1)
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
					this,
					libraryId,
					item,
				)
			}
		}
	}

	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> {
		return if (permissions.isEmpty()) Promise(emptyMap())
		else Promise<Map<String, Boolean>> { messenger ->
			val requestId = messenger.hashCode()
			permissionsRequests[requestId] = messenger

			ActivityCompat.requestPermissions(
				this,
				permissions.toTypedArray(),
				requestId
			)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		permissionsRequests
			.remove(requestCode)
			?.sendResolution(
				grantResults
					.zip(permissions)
					.associate { (r, p) -> Pair(p, r == PackageManager.PERMISSION_GRANTED) }
					.apply {
						if (values.any { !it }) {
							Toast
								.makeText(
									this@ItemBrowserActivity,
									R.string.permissions_must_be_granted_for_settings,
									Toast.LENGTH_LONG
								)
								.show()
						}
					})
	}
}

@OptIn(ExperimentalMaterialApi::class)
private class GraphNavigation(
	private val inner: NavigateApplication,
	private val navController: NavHostController,
	private val bottomSheetState: BottomSheetState,
	private val coroutineScope: CoroutineScope,
	private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
) : NavigateApplication by inner {
	object Library {
		const val route = "library/{$libraryIdArgument}"

		fun buildPath(libraryId: LibraryId) = "library/${libraryId.id}"
	}

	object Settings {
		const val nestedRoute = "settings"

		const val route = "${Library.route}/$nestedRoute"

		fun buildPath(libraryId: LibraryId) = "${Library.buildPath(libraryId)}/${nestedRoute}"
	}

	object Search {
		const val nestedRoute = "search"

		const val route = "${Library.route}/$nestedRoute"

		fun buildPath(libraryId: LibraryId) = "${Library.buildPath(libraryId)}/$nestedRoute"
	}

	object Downloads {
		const val nestedRoute = "downloads"

		const val route = "${Library.route}/$nestedRoute"

		fun buildPath(libraryId: LibraryId) = "${Library.buildPath(libraryId)}/$nestedRoute"
	}

	object BrowseToItem {
		const val nestedRoute =
			"item/{$keyArgument}?$titleArgument={$titleArgument}&$playlistIdArgument={$playlistIdArgument}"

		const val route = "${Library.route}/${nestedRoute}"

		fun buildPath(libraryId: LibraryId, item: IItem): String =
			"${Library.buildPath(libraryId)}/${buildNestedPath(item)}"

		private fun buildNestedPath(item: IItem): String {
			var path = "item/${item.key}?$titleArgument=${item.value}"
			if (item is Item) {
				val playlistId = item.playlistId
				if (playlistId != null)
					path += "&$playlistIdArgument=${playlistId.id}"
			}

			return path
		}
	}

	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		navController.navigate(Search.buildPath(libraryId)) {
			launchSingleTop = true
			popUpTo(BrowseToItem.route)
		}

		hideBottomSheet()
	}.toPromise()

	override fun viewServerSettings(libraryId: LibraryId) = coroutineScope.launch {
		navController.navigate(Settings.buildPath(libraryId)) {
			launchSingleTop = true
			popUpTo(BrowseToItem.route)
		}

		hideBottomSheet()
	}.toPromise()

	override fun viewActiveDownloads(libraryId: LibraryId) = coroutineScope.launch {
		navController.navigate(Downloads.buildPath(libraryId)) {
			launchSingleTop = true
			popUpTo(BrowseToItem.route)
		}

		hideBottomSheet()
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.navigate(Library.buildPath(libraryId)) {
			popUpTo(Library.route)
		}

		hideBottomSheet()
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		navController.navigate(BrowseToItem.buildPath(libraryId, item))
		hideBottomSheet()
	}.toPromise()

	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		hideBottomSheet()
		return inner.viewFileDetails(playlist, position)
	}

	override fun viewNowPlaying(): Promise<Unit> {
		hideBottomSheet()
		return inner.viewNowPlaying()
	}

	override fun navigateUp() = coroutineScope.async {
		hideBottomSheet()

		navController.navigateUp() || inner.navigateUp().suspend()
	}.toPromise()

	override fun backOut() = coroutineScope.async {
		(itemListMenuBackPressedHandler.hideAllMenus() or hideBottomSheet()) || navigateUp().suspend()
	}.toPromise()

	private fun hideBottomSheet(): Boolean {
		if (!bottomSheetState.isCollapsed) {
			coroutineScope.launch { bottomSheetState.collapse() }
			return true
		}

		return false
	}
}

private class GraphDependencies(inner: ItemBrowserViewDependencies, graphNavigation: GraphNavigation) :
	ItemBrowserViewDependencies by inner
{
	override val applicationNavigation = graphNavigation
}

private val bottomAppBarHeight = Dimensions.AppBarHeight

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ItemBrowserView(
	itemBrowserViewDependencies: ItemBrowserViewDependencies,
	startingLibraryId: LibraryId? = null,
	startingItem: IItem? = null,
) {
	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(MaterialTheme.colors.surface)

	val navController = rememberNavController()
	navController.enableOnBackPressed(false)
	val scaffoldState = rememberBottomSheetScaffoldState()
	val coroutineScope = rememberCoroutineScope()

	val bottomSheetState = scaffoldState.bottomSheetState
	val graphNavigation = remember {
		GraphNavigation(
			itemBrowserViewDependencies.applicationNavigation,
			navController,
			bottomSheetState,
			coroutineScope,
			itemBrowserViewDependencies.itemListMenuBackPressedHandler,
		)
	}

	with(remember {
		GraphDependencies(
			itemBrowserViewDependencies,
			graphNavigation,
		)
	}) {
		BottomSheetScaffold(
			scaffoldState = scaffoldState,
			sheetPeekHeight = bottomAppBarHeight,
			sheetElevation = 16.dp,
			sheetContent = {
				Row(
					modifier = Modifier
						.clickable(onClick = applicationNavigation::viewNowPlaying)
						.background(MaterialTheme.colors.secondary)
						.height(bottomAppBarHeight)
				) {
					Column {
						Row(
							modifier = Modifier
								.weight(1f)
								.padding(end = 16.dp)
						) {
							var progress by remember { mutableStateOf(0f) }
							DisposableEffect(key1 = bottomSheetState.offset.value) {
								val bottomSheetProgress = bottomSheetState.progress
								val fraction = bottomSheetProgress.fraction
								val currentState = bottomSheetProgress.from
								progress = when {
									currentState == BottomSheetValue.Collapsed && fraction == 1f -> 0f
									currentState == BottomSheetValue.Collapsed -> fraction
									currentState == BottomSheetValue.Expanded && fraction == 1f -> 1f
									currentState == BottomSheetValue.Expanded -> 1 - fraction
									else -> 0f
								}

								onDispose {  }
							}

							Box(
								modifier = Modifier
									.align(Alignment.CenterVertically)
									.fillMaxHeight()
									.wrapContentWidth()
									.clickable {
										coroutineScope.launch {
											if (bottomSheetState.isExpanded) bottomSheetState.collapse()
											else bottomSheetState.expand()
										}
									},
							) {
								val chevronRotation by remember { derivedStateOf { 180 * progress } }
								Image(
									painter = painterResource(id = R.drawable.chevron_up_white_36dp),
									contentDescription = stringResource(id = if (bottomSheetState.isCollapsed) R.string.show_menu else R.string.hide_menu),
									modifier = Modifier
										.align(Alignment.Center)
										.rotate(chevronRotation)
										.padding(start = 16.dp, end = 16.dp)
										.requiredSize(24.dp)
								)
							}

							Column(
								modifier = Modifier
									.weight(1f)
									.align(Alignment.CenterVertically),
							) {
								val songTitle by nowPlayingFilePropertiesViewModel.title.collectAsState()

								ProvideTextStyle(MaterialTheme.typography.subtitle1) {
									Text(
										text = songTitle
											?: stringResource(id = R.string.lbl_loading),
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										fontWeight = FontWeight.Medium,
										color = MaterialTheme.colors.onSecondary,
									)
								}

								val songArtist by nowPlayingFilePropertiesViewModel.artist.collectAsState()
								ProvideTextStyle(MaterialTheme.typography.body2) {
									Text(
										text = songArtist
											?: stringResource(id = R.string.lbl_loading),
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										color = MaterialTheme.colors.onSecondary,
									)
								}
							}

							val isPlaying by nowPlayingFilePropertiesViewModel.isPlaying.collectAsState()
							Image(
								painter = painterResource(id = if (!isPlaying) R.drawable.av_play_white else R.drawable.av_pause_white),
								contentDescription = stringResource(id = R.string.btn_play),
								modifier = Modifier
									.clickable(
										interactionSource = remember { MutableInteractionSource() },
										indication = null,
										onClick = {
											if (!isPlaying) playbackServiceController.play()
											else playbackServiceController.pause()

											nowPlayingFilePropertiesViewModel.togglePlaying(!isPlaying)
										}
									)
									.padding(start = 8.dp, end = 8.dp)
									.align(Alignment.CenterVertically)
									.size(24.dp),
							)

							Icon(
								Icons.Default.ArrowForward,
								contentDescription = stringResource(id = R.string.title_activity_view_now_playing),
								tint = MaterialTheme.colors.onSecondary,
								modifier = Modifier
									.padding(start = 8.dp, end = 8.dp)
									.align(Alignment.CenterVertically)
							)
						}

						val filePosition by nowPlayingFilePropertiesViewModel.filePosition.collectAsState()
						val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.collectAsState()
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

				val rowHeight = dimensionResource(id = R.dimen.standard_row_height)
				ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
					Row(
						modifier = Modifier
							.height(rowHeight)
							.fillMaxWidth()
							.clickable {
								if (startingLibraryId != null)
									applicationNavigation.viewActiveDownloads(startingLibraryId)
							},
						verticalAlignment = Alignment.CenterVertically,
					) {
						Box(
							modifier = Modifier
								.align(Alignment.CenterVertically)
								.fillMaxHeight()
						) {
							Image(
								painter = painterResource(id = R.drawable.ic_water),
								contentDescription = stringResource(id = R.string.activeDownloads),
								modifier = Modifier
									.align(Alignment.Center)
									.padding(start = 16.dp, end = 16.dp)
							)
						}

						Text(
							text = stringResource(R.string.activeDownloads),
						)
					}

					Row(
						modifier = Modifier
							.height(rowHeight)
							.fillMaxWidth()
							.clickable {
								if (startingLibraryId != null)
									applicationNavigation.launchSearch(startingLibraryId)
							},
						verticalAlignment = Alignment.CenterVertically,
					) {
						Box(
							modifier = Modifier
								.align(Alignment.CenterVertically)
								.fillMaxHeight()
						) {
							Icon(
								Icons.Default.Search,
								contentDescription = stringResource(id = R.string.search),
								modifier = Modifier
									.align(Alignment.Center)
									.padding(start = 16.dp, end = 16.dp)
							)
						}

						Text(
							text = stringResource(R.string.search),
						)
					}

					Row(
						modifier = Modifier
							.height(rowHeight)
							.fillMaxWidth()
							.clickable {
								if (startingLibraryId != null)
									applicationNavigation.viewServerSettings(startingLibraryId)
							},
						verticalAlignment = Alignment.CenterVertically,
					) {
						Box(
							modifier = Modifier
								.align(Alignment.CenterVertically)
								.fillMaxHeight()
						) {
							Image(
								painter = painterResource(id = R.drawable.ic_action_settings),
								contentDescription = stringResource(id = R.string.settings),
								modifier = Modifier
									.align(Alignment.Center)
									.padding(start = 16.dp, end = 16.dp)
									.size(Dimensions.MenuIconSize)
							)
						}

						Text(
							text = stringResource(R.string.settings),
						)
					}
				}
			}
		) { paddingValues ->
			val hostModifier = Modifier
				.padding(paddingValues)
				.fillMaxSize()

			NavHost(
				navController,
				modifier = hostModifier,
				startDestination = GraphNavigation.Library.route,
			) {
				composable(
					GraphNavigation.Library.route,
					arguments = listOf(
						navArgument(libraryIdArgument) {
							type = NavType.IntType
							defaultValue = startingLibraryId?.id ?: -1
						},
					)
				) { entry ->
					val libraryId = entry.arguments?.getInt(libraryIdArgument)?.let(::LibraryId) ?: startingLibraryId ?: return@composable

					val view = browsableItemListView(
						connectionViewModel = entry.viewModelStore.buildViewModel {
							ConnectionStatusViewModel(
								stringResources,
								ConnectionInitializationErrorController(
									ConnectionInitializationProxy(libraryConnectionProvider),
									applicationNavigation,
								),
							)
						},
						itemListViewModel = entry.viewModelStore.buildViewModel {
							ItemListViewModel(
								itemProvider,
								messageBus,
								libraryProvider,
								storedItemAccess,
								itemListProvider,
								playbackServiceController,
								applicationNavigation,
								menuMessageBus,
							)
						},
						fileListViewModel = entry.viewModelStore.buildViewModel {
							FileListViewModel(
								itemFileProvider,
								storedItemAccess,
								playbackServiceController,
							)
						},
						nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
						reusablePlaylistFileItemViewModelProvider = entry.viewModelStore.buildViewModel {
							ReusablePlaylistFileItemViewModelProvider(
								scopedFilePropertiesProvider,
								scopedUrlKeyProvider,
								stringResources,
								playbackServiceController,
								applicationNavigation,
								menuMessageBus,
								messageBus,
							)
						},
						applicationNavigation = applicationNavigation,
					)

					BackHandler { graphNavigation.backOut() }

					view(libraryId, null)
				}

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
					val arguments = entry.arguments ?: return@composable
					val libraryId = LibraryId(arguments.getInt(libraryIdArgument))
					val itemKey = arguments.getInt(keyArgument)
					val playlistId = arguments.getInt(playlistIdArgument)
					val item = if (playlistId > -1) {
						Item(
							itemKey,
							arguments.getString(titleArgument),
							PlaylistId(playlistId),
						)
					} else {
						Item(
							itemKey,
							arguments.getString(titleArgument)
						)
					}

					val view = browsableItemListView(
						connectionViewModel = entry.viewModelStore.buildViewModel {
							ConnectionStatusViewModel(
								stringResources,
								ConnectionInitializationErrorController(
									ConnectionInitializationProxy(libraryConnectionProvider),
									applicationNavigation,
								),
							)
						},
						itemListViewModel = entry.viewModelStore.buildViewModel {
							ItemListViewModel(
								itemProvider,
								messageBus,
								libraryProvider,
								storedItemAccess,
								itemListProvider,
								playbackServiceController,
								applicationNavigation,
								menuMessageBus,
							)
						},
						fileListViewModel = entry.viewModelStore.buildViewModel {
							FileListViewModel(
								itemFileProvider,
								storedItemAccess,
								playbackServiceController,
							)
						},
						nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
						reusablePlaylistFileItemViewModelProvider = entry.viewModelStore.buildViewModel {
							ReusablePlaylistFileItemViewModelProvider(
								scopedFilePropertiesProvider,
								scopedUrlKeyProvider,
								stringResources,
								playbackServiceController,
								applicationNavigation,
								menuMessageBus,
								messageBus,
							)
						},
						applicationNavigation = applicationNavigation,
					)

					BackHandler { graphNavigation.backOut() }

					view(libraryId, item)
				}

				composable(
					GraphNavigation.Downloads.route,
					arguments = listOf(
						navArgument(libraryIdArgument) {
							type = NavType.IntType
							defaultValue = startingLibraryId?.id ?: -1
						},
					)
				) { entry ->
					val libraryId = entry.arguments?.getInt(libraryIdArgument)?.let(::LibraryId) ?: startingLibraryId ?: return@composable
					BackHandler { graphNavigation.backOut() }

					val activeFileDownloadsViewModel = entry.viewModelStore.buildViewModel {
						ActiveFileDownloadsViewModel(
							storedFileAccess,
							messageBus,
							syncScheduler,
						)
					}

					ActiveFileDownloadsView(
						activeFileDownloadsViewModel = activeFileDownloadsViewModel,
						trackHeadlineViewModelProvider =
						entry.viewModelStore.buildViewModel {
							ReusableFileItemViewModelProvider(
								scopedFilePropertiesProvider,
								scopedUrlKeyProvider,
								stringResources,
								messageBus,
							)
						},
						onBack = applicationNavigation::backOut
					)

					activeFileDownloadsViewModel.loadActiveDownloads(libraryId)
				}

				composable(
					GraphNavigation.Search.route,
					arguments = listOf(
						navArgument(libraryIdArgument) {
							type = NavType.IntType
							defaultValue = startingLibraryId?.id ?: -1
						},
					)
				) { entry ->
					val libraryId = entry.arguments?.getInt(libraryIdArgument)?.let(::LibraryId) ?: startingLibraryId ?: return@composable

					BackHandler { graphNavigation.backOut() }

					val searchFilesViewModel = entry.viewModelStore.buildViewModel {
						SearchFilesViewModel(
							libraryFilesProvider,
							playbackServiceController,
						)
					}

					searchFilesViewModel.setActiveLibraryId(libraryId)

					SearchFilesView(
						searchFilesViewModel = searchFilesViewModel,
						nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
						trackHeadlineViewModelProvider = entry.viewModelStore.buildViewModel {
							ReusablePlaylistFileItemViewModelProvider(
								scopedFilePropertiesProvider,
								scopedUrlKeyProvider,
								stringResources,
								playbackServiceController,
								applicationNavigation,
								menuMessageBus,
								messageBus,
							)
						},
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
						onBack = applicationNavigation::backOut,
					)
				}

				composable(
					GraphNavigation.Settings.route,
					arguments = listOf(
						navArgument(libraryIdArgument) {
							type = NavType.IntType
							defaultValue = startingLibraryId?.id ?: -1
						},
					)
				) { entry ->
					val libraryId = entry.arguments?.getInt(libraryIdArgument)?.let(::LibraryId) ?: startingLibraryId ?: return@composable
					BackHandler { graphNavigation.backOut() }

					val viewModel = entry.viewModelStore.buildViewModel {
						LibrarySettingsViewModel(
							libraryProvider = libraryProvider,
							libraryStorage = libraryStorage,
							libraryRemoval = libraryRemoval,
							applicationReadPermissionsRequirementsProvider = readPermissionsRequirements,
							applicationWritePermissionsRequirementsProvider = writePermissionsRequirements,
							permissionsManager = permissionsManager,
						)
					}

					LibrarySettingsView(
						librarySettingsViewModel = viewModel,
						navigateApplication = graphNavigation,
						stringResources = stringResources
					)

					viewModel.loadLibrary(libraryId)

					DisposableEffect(key1 = Unit) {
						val registration = messageBus.registerReceiver(coroutineScope) { m: ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated ->
							if (libraryId == m.libraryId)
								graphNavigation.viewLibrary(libraryId)
						}

						onDispose {
							registration.close()
						}
					}
				}
			}
		}
	}
}
