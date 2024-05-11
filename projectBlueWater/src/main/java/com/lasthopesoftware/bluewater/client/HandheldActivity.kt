@file:OptIn(ExperimentalFoundationApi::class)

package com.lasthopesoftware.bluewater.client

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import browsableItemListView
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedBrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesView
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DestinationApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryMenu
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializingLibrarySelectionNavigation
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val logger by lazyLogger<HandheldActivity>()
private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<HandheldActivity>()) }
private val cachedDestinationActions = ConcurrentHashMap<Class<*>, String>()

val destinationProperty by lazy { magicPropertyBuilder.buildProperty("destination") }
fun destinationAction(destination: Destination): String = cachedDestinationActions.getOrPut(destination.javaClass) { "$destinationProperty/${destination.javaClass.name}" }

@UnstableApi class HandheldActivity :
	AppCompatActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions,
	PermissionsDependencies,
	ActivitySuppliedDependencies
{
	private val browserViewDependencies by lazy { ActivityDependencies(this, this) }

	override val registeredActivityResultsLauncher = registerResultActivityLauncher()

	override val applicationPermissions by lazy {
		val osPermissionChecker = OsPermissionsChecker(applicationContext)
		ApplicationPermissionsRequests(
			browserViewDependencies.libraryProvider,
			ApplicationReadPermissionsRequirementsProvider(osPermissionChecker),
			this,
			osPermissionChecker
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

		applicationPermissions.promiseApplicationPermissionsRequest()

		WindowCompat.setDecorFitsSystemWindows(window, false)

		setContent {
			ProjectBlueTheme {
				BrowserView(browserViewDependencies, this, getDestination(intent))
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		this.intent = intent

		getDestination(intent)?.also { browserViewDependencies.navigationMessages.sendMessage(NavigationMessage(it)) }
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
			)
	}

	private fun getDestination(intent: Intent?) =
		intent?.safelyGetParcelableExtra<Destination>(destinationProperty)
}

private class SkipNowPlayingNavigation(private val inner: NavigateApplication): NavigateApplication by inner {
	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> = Unit.toPromise()
}

@OptIn(ExperimentalCoroutinesApi::class)
private class GraphNavigation(
	private val inner: NavigateApplication,
	private val navController: NavController<Destination>,
	private val coroutineScope: CoroutineScope,
	private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) : NavigateApplication by inner {

	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(SearchScreen(libraryId))
	}.toPromise()

	override fun viewApplicationSettings() = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }
	}.toPromise()

	override fun viewHiddenSettings(): Promise<Unit> = coroutineScope.launch {
		navController.navigate(HiddenSettingsScreen)
	}.toPromise()

	override fun viewNewServerSettings() = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(NewConnectionSettingsScreen)
	}.toPromise()

	override fun viewServerSettings(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(ConnectionSettingsScreen(libraryId))
	}.toPromise()

	override fun viewActiveDownloads(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(DownloadsScreen(libraryId))
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		if (item is Item)
			navController.navigate(ItemScreen(libraryId, item))
	}.toPromise()

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		return inner.viewFileDetails(libraryId, playlist, position)
	}

	override fun viewNowPlaying(libraryId: LibraryId) = coroutineScope.launch {
		if (!navController.moveToTop { it is NowPlayingScreen }) {
			navController.navigate(NowPlayingScreen(libraryId))
		}
	}.toPromise()

	override fun navigateUp() = coroutineScope.async {
		(navController.pop() && navController.backstack.entries.any()) || inner.navigateUp().suspend()
	}.toPromise()

	override fun backOut() = coroutineScope.async {
		itemListMenuBackPressedHandler.hideAllMenus() || navigateUp().suspend()
	}.toPromise()
}

@OptIn(ExperimentalMaterialApi::class)
private class BottomSheetHidingNavigation(
	private val inner: NavigateApplication,
	private val bottomSheetState: BottomSheetState,
	private val coroutineScope: CoroutineScope,
	private val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) : NavigateApplication by inner {

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.launchSearch(libraryId)
	}

	override fun viewServerSettings(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewServerSettings(libraryId)
	}

	override fun viewActiveDownloads(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewActiveDownloads(libraryId)
	}

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewLibrary(libraryId)
	}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> {
		hideBottomSheet()

		return inner.viewItem(libraryId, item)
	}

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		hideBottomSheet()
		return inner.viewFileDetails(libraryId, playlist, position)
	}

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> {
		hideBottomSheet()

		return inner.viewNowPlaying(libraryId)
	}

	override fun navigateUp(): Promise<Boolean> {
		hideBottomSheet()

		return inner.navigateUp()
	}

	override fun backOut(): Promise<Boolean> {
		val isHidden = itemListMenuBackPressedHandler.hideAllMenus() or hideBottomSheet()

		return if (isHidden) true.toPromise()
		else inner.backOut()
	}

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
	graphNavigation: NavigateApplication,
	override val connectionStatusViewModel: ConnectionStatusViewModel,
	navController: NavController<Destination>,
	initialDestination: Destination?
) : BrowserViewDependencies by inner, AutoCloseable {
	private val closeableManager = AutoCloseableManager()

	override val applicationNavigation by lazy {
		closeableManager.manage(
			DestinationApplicationNavigation(
				ConnectionInitializingLibrarySelectionNavigation(
					graphNavigation,
					selectedLibraryViewModel,
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

private val bottomAppBarHeight = Dimensions.appBarHeight
private val bottomSheetElevation = 16.dp

@Composable
fun InitializedBrowserView(viewModelDependencies: ScopedBrowserViewDependencies, libraryId: LibraryId, item: Item?) {
	with (viewModelDependencies) {
		val view = browsableItemListView(
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

		ViewModelInitAction {
			view(libraryId, item)
		}
	}
}

@Composable
fun BrowserLibraryDestination.Navigate(browserViewDependencies: ScopedBrowserViewDependencies) {
	with(browserViewDependencies) {
		when (this@Navigate) {
			is LibraryScreen -> {
				InitializedBrowserView(browserViewDependencies, libraryId, null)
			}

			is ItemScreen -> {
				InitializedBrowserView(browserViewDependencies, libraryId, item)
			}

			is DownloadsScreen -> {
				ActiveFileDownloadsView(
					activeFileDownloadsViewModel = activeFileDownloadsViewModel,
					trackHeadlineViewModelProvider = reusableFileItemViewModelProvider,
					applicationNavigation,
				)

				activeFileDownloadsViewModel.loadActiveDownloads(libraryId)
			}

			is SearchScreen -> {
				searchFilesViewModel.setActiveLibraryId(libraryId)

				SearchFilesView(
					searchFilesViewModel = searchFilesViewModel,
					nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
					trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					applicationNavigation = applicationNavigation,
					playbackServiceController = playbackServiceController,
				)
			}
		}
	}
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun BrowserLibraryDestination.Navigate(
	browserViewDependencies: ScopedBrowserViewDependencies,
	scaffoldState: BottomSheetScaffoldState,
) {
	with(browserViewDependencies) {
		val selectedLibraryId by selectedLibraryViewModel.selectedLibraryId.collectAsState()
		val isSelectedLibrary by remember { derivedStateOf { selectedLibraryId == libraryId } }

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
		BottomSheetScaffold(
			modifier = Modifier
				.fillMaxSize()
				.padding(systemBarsPadding),
			scaffoldState = scaffoldState,
			sheetPeekHeight = if (isSelectedLibrary) bottomAppBarHeight else 0.dp,
			sheetElevation = bottomSheetElevation,
			sheetContent = {
				if (isSelectedLibrary) {
					LibraryMenu(
						applicationNavigation = applicationNavigation,
						nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
						playbackServiceController = playbackServiceController,
						bottomSheetState = scaffoldState.bottomSheetState,
						libraryId = libraryId,
					)

					val context = LocalContext.current
					LaunchedEffect(key1 = libraryId) {
						try {
							nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId).suspend()
						} catch (e: Throwable) {
							if (ConnectionLostExceptionFilter.isConnectionLostException(e))
								pollForConnections.pollConnection(libraryId)
							else
								UnexpectedExceptionToaster.announce(context, e)
						}
					}
				}
			}
		) { paddingValues ->
			Box(modifier = Modifier.padding(paddingValues)) { Navigate(browserViewDependencies) }
		}
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LibraryDestination.Navigate(browserViewDependencies: ScopedBrowserViewDependencies) {
	with(browserViewDependencies) {
		when (this@Navigate) {
			is BrowserLibraryDestination -> {
				Row(modifier = Modifier.fillMaxSize()) {
					Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
					) {
						Navigate(browserViewDependencies)
					}

					Box(
						modifier = Modifier
							.weight(2f)
							.fillMaxHeight()
					) {
						NowPlayingView(
							nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
							nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
							screenOnState = nowPlayingScreenViewModel,
							playbackServiceController = playbackServiceController,
							playlistViewModel = nowPlayingPlaylistViewModel,
							childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
							applicationNavigation = applicationNavigation,
							itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
							connectionWatcherViewModel = connectionWatcherViewModel,
							viewModelMessageBus = nowPlayingViewModelMessageBus
						)

						val context = LocalContext.current
						LaunchedEffect(key1 = libraryId) {
							try {
								connectionWatcherViewModel.watchLibraryConnection(libraryId)

								Promise.whenAll(
									nowPlayingScreenViewModel.initializeViewModel(libraryId),
									nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId),
									nowPlayingCoverArtViewModel.initializeViewModel(libraryId),
									nowPlayingPlaylistViewModel.initializeView(libraryId),
								).suspend()
							} catch (e: Throwable) {
								if (ConnectionLostExceptionFilter.isConnectionLostException(e))
									pollForConnections.pollConnection(libraryId)
								else
									UnexpectedExceptionToaster.announce(context, e)
							}
						}
					}
				}
			}

			is ConnectionSettingsScreen -> {
				val viewModel = librarySettingsViewModel

				val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(systemBarsPadding)
				) {
					LibrarySettingsView(
						librarySettingsViewModel = viewModel,
						navigateApplication = applicationNavigation,
						stringResources = stringResources,
						userSslCertificates = userSslCertificateProvider,
					)
				}

				viewModel.loadLibrary(libraryId)
			}

			is NowPlayingScreen -> {}
		}
	}
}

@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
fun LibraryDestination.Navigate(
	browserViewDependencies: ScopedBrowserViewDependencies,
	scaffoldState: BottomSheetScaffoldState,
) {
	with(browserViewDependencies) {
		when (this@Navigate) {
			is BrowserLibraryDestination -> {
				Navigate(
					browserViewDependencies = browserViewDependencies,
					scaffoldState = scaffoldState,
				)
			}

			is ConnectionSettingsScreen -> {
				val viewModel = librarySettingsViewModel

				val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(systemBarsPadding)
				) {
					LibrarySettingsView(
						librarySettingsViewModel = viewModel,
						navigateApplication = applicationNavigation,
						stringResources = stringResources,
						userSslCertificates = userSslCertificateProvider,
					)
				}

				viewModel.loadLibrary(libraryId)
			}

			is NowPlayingScreen -> {
				val systemUiController = rememberSystemUiController()
				systemUiController.setSystemBarsColor(SharedColors.overlayDark)

				val screenViewModel = viewModel {
					NowPlayingScreenViewModel(
						messageBus,
						InMemoryNowPlayingDisplaySettings,
						playbackServiceController,
					)
				}

				NowPlayingView(
					nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					screenOnState = screenViewModel,
					playbackServiceController = playbackServiceController,
					playlistViewModel = nowPlayingPlaylistViewModel,
					childItemViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					connectionWatcherViewModel = connectionWatcherViewModel,
					viewModelMessageBus = nowPlayingViewModelMessageBus
				)

				val context = LocalContext.current
				LaunchedEffect(key1 = libraryId) {
					try {
						connectionWatcherViewModel.watchLibraryConnection(libraryId)

						Promise.whenAll(
							screenViewModel.initializeViewModel(libraryId),
							nowPlayingFilePropertiesViewModel.initializeViewModel(libraryId),
							nowPlayingCoverArtViewModel.initializeViewModel(libraryId),
							nowPlayingPlaylistViewModel.initializeView(libraryId),
						).suspend()
					} catch (e: Throwable) {
						if (ConnectionLostExceptionFilter.isConnectionLostException(e))
							pollForConnections.pollConnection(libraryId)
						else
							UnexpectedExceptionToaster.announce(context, e)
					}
				}
			}
		}
	}
}

@Composable
private fun UnifiedClientView(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination?
) {
	val systemUiController = rememberSystemUiController()

	val navController = rememberNavController(
		if (initialDestination == null) listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
		else listOf(ApplicationSettingsScreen)
	)

	val coroutineScope = rememberCoroutineScope()

	val graphNavigation = remember {
		SkipNowPlayingNavigation(
			GraphNavigation(
				browserViewDependencies.applicationNavigation,
				navController,
				coroutineScope,
				browserViewDependencies.itemListMenuBackPressedHandler
			)
		)
	}

	val connectionStatusViewModel = viewModel {
		ConnectionStatusViewModel(
			browserViewDependencies.stringResources,
			DramaticConnectionInitializationController(
				browserViewDependencies.libraryConnectionProvider,
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

	val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

	ControlSurface {
		NavHost(navController) { destination ->
			systemUiController.setStatusBarColor(MaterialTheme.colors.surface)
			systemUiController.setNavigationBarColor(Color.Black)

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
					LocalViewModelStoreOwner.current?.also {
						destination.Navigate(
							ScopedViewModelDependencies(graphDependencies, permissionsDependencies, it)
						)
					}
				}
				is ApplicationSettingsScreen -> {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(systemBarsPadding)
					) {
						ApplicationSettingsView(
							applicationSettingsViewModel = graphDependencies.applicationSettingsViewModel,
							applicationNavigation = graphDependencies.applicationNavigation,
							playbackService = graphDependencies.playbackServiceController,
						)
					}

					graphDependencies.applicationSettingsViewModel.loadSettings()
				}
				is NewConnectionSettingsScreen -> {
					LocalViewModelStoreOwner.current
						?.let {
							ScopedViewModelDependencies(graphDependencies, permissionsDependencies, it)
						}
						?.apply {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.padding(systemBarsPadding)
							) {
								LibrarySettingsView(
									librarySettingsViewModel = librarySettingsViewModel,
									navigateApplication = applicationNavigation,
									stringResources = stringResources,
									userSslCertificates = userSslCertificateProvider,
								)
							}
						}
				}
				is HiddenSettingsScreen -> {
					HiddenSettingsView(graphDependencies.hiddenSettingsViewModel)

					graphDependencies.hiddenSettingsViewModel.loadApplicationSettings()
				}
			}
		}

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
		if (isCheckingConnection) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(systemBarsPadding)
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun NestedClientView(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination?
) {
	val systemUiController = rememberSystemUiController()

	val navController = rememberNavController(
		if (initialDestination == null) listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
		else listOf(ApplicationSettingsScreen)
	)
	val scaffoldState = rememberBottomSheetScaffoldState()
	val coroutineScope = rememberCoroutineScope()

	val bottomSheetState = scaffoldState.bottomSheetState
	val graphNavigation = remember {
		BottomSheetHidingNavigation(
			GraphNavigation(
				browserViewDependencies.applicationNavigation,
				navController,
				coroutineScope,
				browserViewDependencies.itemListMenuBackPressedHandler
			),
			bottomSheetState,
			coroutineScope,
			browserViewDependencies.itemListMenuBackPressedHandler
		)
	}

	val connectionStatusViewModel = viewModel {
		ConnectionStatusViewModel(
			browserViewDependencies.stringResources,
			DramaticConnectionInitializationController(
				browserViewDependencies.libraryConnectionProvider,
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

	val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

	ControlSurface {
		NavHost(navController) { destination ->
			systemUiController.setStatusBarColor(MaterialTheme.colors.surface)
			systemUiController.setNavigationBarColor(Color.Black)

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
					LocalViewModelStoreOwner.current?.also {
						destination.Navigate(
							ScopedViewModelDependencies(graphDependencies, permissionsDependencies, it),
							scaffoldState,
						)
					}
				}
				is ApplicationSettingsScreen -> {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(systemBarsPadding)
					) {
						ApplicationSettingsView(
							applicationSettingsViewModel = graphDependencies.applicationSettingsViewModel,
							applicationNavigation = graphDependencies.applicationNavigation,
							playbackService = graphDependencies.playbackServiceController,
						)
					}

					graphDependencies.applicationSettingsViewModel.loadSettings()
				}
				is NewConnectionSettingsScreen -> {
					LocalViewModelStoreOwner.current
						?.let {
							ScopedViewModelDependencies(graphDependencies, permissionsDependencies, it)
						}
						?.apply {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.padding(systemBarsPadding)
							) {
								LibrarySettingsView(
									librarySettingsViewModel = librarySettingsViewModel,
									navigateApplication = applicationNavigation,
									stringResources = stringResources,
									userSslCertificates = userSslCertificateProvider,
                                )
							}
						}
				}
				is HiddenSettingsScreen -> {
					HiddenSettingsView(graphDependencies.hiddenSettingsViewModel)

					graphDependencies.hiddenSettingsViewModel.loadApplicationSettings()
				}
			}
		}

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
		if (isCheckingConnection) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(systemBarsPadding)
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}

@Composable
private fun BrowserView(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination? = null
) {
	BoxWithConstraints(
		modifier = Modifier.fillMaxSize()
	) {
		if (maxWidth > Dimensions.threeColumnThreshold) {
			UnifiedClientView(
				browserViewDependencies = browserViewDependencies,
				permissionsDependencies = permissionsDependencies,
				initialDestination = initialDestination,
			)
		} else {
			NestedClientView(
				browserViewDependencies = browserViewDependencies,
				permissionsDependencies = permissionsDependencies,
				initialDestination = initialDestination,
			)
		}
	}
}
