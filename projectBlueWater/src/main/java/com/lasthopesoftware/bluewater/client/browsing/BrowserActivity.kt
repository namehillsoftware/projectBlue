package com.lasthopesoftware.bluewater.client.browsing

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import browsableItemListView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.about.AboutView
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.CachedItemFileProvider
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
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.*
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.navigation.*
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.initialization.*
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
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
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.*
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.lasthopesoftware.resources.closables.ViewModelCloseableManager
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val logger by lazyLogger<BrowserActivity>()
private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<BrowserActivity>()) }
private val cachedDestinationActions = ConcurrentHashMap<Class<*>, String>()

val destinationProperty by lazy { magicPropertyBuilder.buildProperty("destination") }
fun destinationAction(destination: Destination): String = cachedDestinationActions.getOrPut(destination.javaClass) { "$destinationProperty(${destination.javaClass.name})" }

class BrowserActivity :
	AppCompatActivity(),
	BrowserViewDependencies,
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions {

	private val viewModelScope by buildViewModelLazily { ViewModelCloseableManager() }

	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(libraryConnectionProvider) }

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
				PromisingRateLimiter(1),
			),
		)
	}

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

	private val itemListProvider by lazy {
		ItemStringListProvider(
			FileListParameters,
			libraryFileStringListProvider
		)
	}

	override val selectedLibraryIdProvider by lazy { getCachedSelectedLibraryIdProvider() }

	override val messageBus by lazy { getApplicationMessageBus().getScopedMessageBus().also(viewModelScope::manage) }

	override val menuMessageBus by buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	override val itemListMenuBackPressedHandler by lazyScoped { ItemListMenuBackPressedHandler(menuMessageBus) }

	override val itemProvider by lazy { CachedItemProvider.getInstance(applicationContext) }

	override val itemFileProvider by lazy { CachedItemFileProvider.getInstance(applicationContext) }

	override val scopedFilePropertiesProvider by lazy {
		SelectedLibraryFilePropertiesProvider(
			selectedLibraryIdProvider,
			libraryFilePropertiesProvider,
		)
	}

	override val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			selectedLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider),
		)
	}

	override val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	override val playbackServiceController by lazy { PlaybackServiceController(applicationContext) }

	override val nowPlayingFilePropertiesViewModel by buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			messageBus,
			LiveNowPlayingLookup.getInstance(),
			libraryFilePropertiesProvider,
			UrlKeyProvider(libraryConnectionProvider),
			filePropertiesStorage,
			connectionAuthenticationChecker,
			playbackServiceController,
			ConnectionPoller(applicationContext),
			stringResources,
		).apply { initializeViewModel() }
	}

	override val storedItemAccess by lazy {
		StateChangeBroadcastingStoredItemAccess(StoredItemAccess(applicationContext), messageBus)
	}

	override val storedFileAccess by lazy { StoredFileAccess(applicationContext) }

	override val stringResources by lazy { StringResources(applicationContext) }

	override val libraryFilesProvider by lazy {
		LibraryFileProvider(LibraryFileStringListProvider(libraryConnectionProvider))
	}

	override val applicationNavigation: NavigateApplication by lazy {
		ActivityApplicationNavigation(
			this,
			IntentBuilder(applicationContext),
		)
	}

	override val syncScheduler by lazy { SyncScheduler(applicationContext) }

	private val libraryRepository by lazy { LibraryRepository(applicationContext) }

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
			selectedLibraryIdProvider,
			libraryRepository,
			BrowserLibrarySelection(applicationSettingsRepository, messageBus, libraryProvider),
		)
	}

	override val readPermissionsRequirements by lazy { ApplicationReadPermissionsRequirementsProvider(applicationContext) }
	override val writePermissionsRequirements by lazy {
		ApplicationWritePermissionsRequirementsProvider(
			applicationContext
		)
	}
	override val permissionsManager = this
	override val navigationMessages by buildViewModelLazily { ViewModelMessageBus<NavigationMessage>() }
	override val applicationSettingsRepository by lazy { applicationContext.getApplicationSettingsRepository() }
	override val selectedPlaybackEngineTypeAccess by lazy {
		SelectedPlaybackEngineTypeAccess(
			applicationSettingsRepository,
			DefaultPlaybackEngineLookup
		)
	}
	override val libraryBrowserSelection by lazy {
		BrowserLibrarySelection(
			applicationSettingsRepository,
			messageBus,
			libraryProvider,
		)
	}

	override val playbackLibraryItems by lazy {
		ItemPlayback(
			itemListProvider,
			playbackServiceController
		)
	}

	private val permissionsRequests = ConcurrentHashMap<Int, Messenger<Map<String, Boolean>>>()

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Ensure that this task is only started when it's the task root. A workaround for an Android bug.
		// See http://stackoverflow.com/a/7748416
		val intent = intent
		if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
			if (!isTaskRoot) {
				val className = javaClass.name
				logger.info("$className is not the root.  Finishing $className instead of launching.")
				finish()
				return
			}
		}

		setContent {
			ProjectBlueTheme {
				BrowserView(this, getDestination(intent))
			}
		}
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)

		this.intent = intent

		getDestination(intent)?.also { navigationMessages.sendMessage(NavigationMessage(it)) }
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
									this@BrowserActivity,
									R.string.permissions_must_be_granted_for_settings,
									Toast.LENGTH_LONG
								)
								.show()
						}
					})
	}

	private fun getDestination(intent: Intent?) =
		intent?.safelyGetParcelableExtra<Destination>(destinationProperty)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
private class GraphNavigation(
	private val inner: NavigateApplication,
	private val navController: NavController<Destination>,
	private val bottomSheetState: BottomSheetState,
	private val coroutineScope: CoroutineScope,
	private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) : NavigateApplication by inner {

	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(SearchScreen(libraryId))

		hideBottomSheet()
	}.toPromise()

	override fun viewApplicationSettings() = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }
	}.toPromise()

	override fun viewNewServerSettings() = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(NewConnectionSettingsScreen)
	}.toPromise()

	override fun viewServerSettings(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(ConnectionSettingsScreen(libraryId))

		hideBottomSheet()
	}.toPromise()

	override fun viewActiveDownloads(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(DownloadsScreen(libraryId))

		hideBottomSheet()
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))

		hideBottomSheet()
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		if (item is Item)
			navController.navigate(ItemScreen(libraryId, item))

		hideBottomSheet()
	}.toPromise()

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		hideBottomSheet()
		return inner.viewFileDetails(libraryId, playlist, position)
	}

	override fun viewNowPlaying(): Promise<Unit> {
		hideBottomSheet()
		return inner.viewNowPlaying()
	}

	override fun launchAboutActivity() = coroutineScope.launch {
		navController.navigate(AboutScreen)
	}.toPromise()

	override fun navigateUp() = coroutineScope.async {
		hideBottomSheet()

		(navController.pop() && navController.backstack.entries.any()) || inner.navigateUp().suspend()
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

private class GraphDependencies(
	inner: BrowserViewDependencies,
	graphNavigation: GraphNavigation,
	connectionStatusViewModel: ConnectionStatusViewModel,
	navController: NavController<Destination>,
	initialDestination: Destination?
) : BrowserViewDependencies by inner, AutoCloseable {
	private val closeableManager = AutoCloseableManager()

	override val applicationNavigation by lazy {
		closeableManager.manage(
			DestinationApplicationNavigation(
				ConnectionInitializingLibrarySelectionNavigation(
					graphNavigation,
					libraryBrowserSelection,
					connectionStatusViewModel,
				),
				navController,
				navigationMessages,
				initialDestination
			)
		)
	}

	override fun close() {
		closeableManager.close()
	}
}

private val bottomAppBarHeight = Dimensions.AppBarHeight

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun LibraryDestination.Navigate(
	browserViewDependencies: BrowserViewDependencies,
	connectionStatusViewModel: ConnectionStatusViewModel,
	scaffoldState: BottomSheetScaffoldState,
	coroutineScope: CoroutineScope,
) {
	val screen = this

	val testedLibraryId by connectionStatusViewModel.testedLibraryId.collectAsState()

	with(browserViewDependencies) {
		BottomSheetScaffold(
			scaffoldState = scaffoldState,
			sheetPeekHeight = if (testedLibraryId == libraryId) bottomAppBarHeight else 0.dp,
			sheetElevation = 16.dp,
			sheetContent = {
				if (testedLibraryId == libraryId) {
					LibraryMenu(
						applicationNavigation = applicationNavigation,
						nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
						playbackServiceController = playbackServiceController,
						bottomSheetState = scaffoldState.bottomSheetState,
						libraryId = libraryId,
					)
				}
			}
		) { paddingValues ->
			Box(modifier = Modifier.padding(paddingValues)) {
				when (screen) {
					is LibraryScreen -> {
						val view = browsableItemListView(
							itemListViewModel = viewModel {
								ItemListViewModel(
									itemProvider,
									messageBus,
									libraryProvider,
								)
							},
							fileListViewModel = viewModel {
								FileListViewModel(
									itemFileProvider,
									storedItemAccess,
								)
							},
							nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
							itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
							reusablePlaylistFileItemViewModelProvider = viewModel {
								ReusablePlaylistFileItemViewModelProvider(
									scopedFilePropertiesProvider,
									scopedUrlKeyProvider,
									stringResources,
									menuMessageBus,
									messageBus,
								)
							},
							childItemViewModelProvider = viewModel {
								ReusableChildItemViewModelProvider(
									storedItemAccess,
									menuMessageBus,
								)
							},
							applicationNavigation = applicationNavigation,
							playbackLibraryItems = playbackLibraryItems,
							playbackServiceController = playbackServiceController,
						)

						view(screen.libraryId, null)
					}
					is ItemScreen -> {
						val view = browsableItemListView(
							itemListViewModel = viewModel {
								ItemListViewModel(
									itemProvider,
									messageBus,
									libraryProvider,
								)
							},
							fileListViewModel = viewModel {
								FileListViewModel(
									itemFileProvider,
									storedItemAccess,
								)
							},
							nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
							itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
							reusablePlaylistFileItemViewModelProvider = viewModel {
								ReusablePlaylistFileItemViewModelProvider(
									scopedFilePropertiesProvider,
									scopedUrlKeyProvider,
									stringResources,
									menuMessageBus,
									messageBus,
								)
							},
							childItemViewModelProvider = viewModel {
								ReusableChildItemViewModelProvider(
									storedItemAccess,
									menuMessageBus,
								)
							},
							applicationNavigation = applicationNavigation,
							playbackLibraryItems = playbackLibraryItems,
							playbackServiceController = playbackServiceController,
						)

						view(screen.libraryId, screen.item)
					}
					is DownloadsScreen -> {
						val activeFileDownloadsViewModel = viewModel {
							ActiveFileDownloadsViewModel(
								storedFileAccess,
								messageBus,
								syncScheduler,
							)
						}

						ActiveFileDownloadsView(
							activeFileDownloadsViewModel = activeFileDownloadsViewModel,
							trackHeadlineViewModelProvider = viewModel {
								ReusableFileItemViewModelProvider(
									scopedFilePropertiesProvider,
									scopedUrlKeyProvider,
									stringResources,
									messageBus,
								)
							},
							applicationNavigation,
						)

						activeFileDownloadsViewModel.loadActiveDownloads(screen.libraryId)
					}
					is SearchScreen -> {
						val searchFilesViewModel = viewModel { SearchFilesViewModel(libraryFilesProvider) }

						searchFilesViewModel.setActiveLibraryId(screen.libraryId)

						SearchFilesView(
							searchFilesViewModel = searchFilesViewModel,
							nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
							trackHeadlineViewModelProvider = viewModel {
								ReusablePlaylistFileItemViewModelProvider(
									scopedFilePropertiesProvider,
									scopedUrlKeyProvider,
									stringResources,
									menuMessageBus,
									messageBus,
								)
							},
							itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
							applicationNavigation = applicationNavigation,
							playbackServiceController = playbackServiceController,
						)
					}
					is ConnectionSettingsScreen -> {
						val viewModel = viewModel {
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
							navigateApplication = applicationNavigation,
							stringResources = stringResources
						)

						viewModel.loadLibrary(screen.libraryId)

						DisposableEffect(key1 = Unit) {
							val registration =
								messageBus.registerReceiver(coroutineScope) { m: ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated ->
									if (screen.libraryId == m.libraryId)
										applicationNavigation.viewLibrary(screen.libraryId)
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
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun BrowserView(
	browserViewDependencies: BrowserViewDependencies,
	initialDestination: Destination? = null
) {
	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(MaterialTheme.colors.surface)

	val navController = rememberNavController(
		if (initialDestination == null) listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
		else listOf(ApplicationSettingsScreen)
	)
	val scaffoldState = rememberBottomSheetScaffoldState()
	val coroutineScope = rememberCoroutineScope()

	val bottomSheetState = scaffoldState.bottomSheetState
	val graphNavigation = remember {
		GraphNavigation(
			browserViewDependencies.applicationNavigation,
			navController,
			bottomSheetState,
			coroutineScope,
			browserViewDependencies.itemListMenuBackPressedHandler
		)
	}

	val connectionStatusViewModel = viewModel {
		ConnectionStatusViewModel(
			browserViewDependencies.stringResources,
			ConnectionInitializationErrorController(
				DramaticConnectionInitializationProxy(
					browserViewDependencies.libraryConnectionProvider,
				),
				graphNavigation,
			),
		)
	}

	val graphDependencies = remember {
		GraphDependencies(
			browserViewDependencies,
			graphNavigation,
			connectionStatusViewModel,
			navController,
			initialDestination
		)
	}

	DisposableEffect(key1 = graphDependencies) {
		onDispose {
			graphDependencies.close()
		}
	}

	BackHandler { graphDependencies.applicationNavigation.backOut() }

	Box(modifier = Modifier.fillMaxSize()) {
		Surface {
			NavHost(navController) { destination ->
				when (destination) {
					is SelectedLibraryReRouter -> {
						graphDependencies.apply {
							LaunchedEffect(key1 = Unit) {
								try {
									val settings =
										applicationSettingsRepository.promiseApplicationSettings().suspend()
									if (settings.chosenLibraryId > -1) {
										val libraryId = LibraryId(settings.chosenLibraryId)
										applicationNavigation.viewLibrary(libraryId).suspend()
										return@LaunchedEffect
									}
								} catch (e: Throwable) {
									logger.error("An error occurred initializing the library", e)
								}

								applicationNavigation.backOut().suspend()
							}
						}
					}
					is ActiveLibraryDownloadsScreen -> {
						graphDependencies.apply {
							LaunchedEffect(key1 = Unit) {
								try {
									val settings =
										applicationSettingsRepository.promiseApplicationSettings().suspend()
									if (settings.chosenLibraryId > -1) {
										val libraryId = LibraryId(settings.chosenLibraryId)
										applicationNavigation.viewLibrary(libraryId).suspend()
										applicationNavigation.viewActiveDownloads(libraryId).suspend()
										return@LaunchedEffect
									}
								} catch (e: Throwable) {
									logger.error("An error occurred initializing the library", e)
								}

								applicationNavigation.backOut().suspend()
							}
						}
					}
					is LibraryDestination -> {
						destination.Navigate(
							graphDependencies,
							connectionStatusViewModel,
							scaffoldState,
							coroutineScope,
						)
					}
					is ApplicationSettingsScreen -> {
						val viewModel = viewModel {
							ApplicationSettingsViewModel(
								graphDependencies.applicationSettingsRepository,
								graphDependencies.selectedPlaybackEngineTypeAccess,
								graphDependencies.libraryProvider,
								graphDependencies.messageBus,
								graphDependencies.syncScheduler,
							)
						}

						ApplicationSettingsView(
							applicationSettingsViewModel = viewModel,
							applicationNavigation = graphDependencies.applicationNavigation,
							playbackService = graphDependencies.playbackServiceController,
						)

						viewModel.loadSettings()
					}
					is NewConnectionSettingsScreen -> {
						with(graphDependencies) {
							val viewModel = viewModel {
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
								navigateApplication = applicationNavigation,
								stringResources = stringResources
							)
						}
					}
					is AboutScreen -> {
						AboutView(graphDependencies.applicationNavigation)
					}
					is HiddenSettingsScreen -> {
						HiddenSettingsView(viewModel {
							HiddenSettingsViewModel(graphDependencies.applicationSettingsRepository)
						})
					}
				}
			}

			val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
			if (isCheckingConnection) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
