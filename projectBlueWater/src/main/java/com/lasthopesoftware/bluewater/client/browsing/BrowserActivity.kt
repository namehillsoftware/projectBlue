package com.lasthopesoftware.bluewater.client.browsing

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import browsableItemListView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.about.AboutView
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.*
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.*
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.*
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.initialization.*
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsView
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.client.stored.library.permissions.folder.WritableFoldersProvider
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsView
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsViewModel
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.*
import com.lasthopesoftware.resources.closables.AutoCloseableManager
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
fun destinationAction(destination: Destination): String = cachedDestinationActions.getOrPut(destination.javaClass) { "$destinationProperty/${destination.javaClass.name}" }

class BrowserActivity :
	AppCompatActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions,
	PermissionsDependencies
{
	private val browserViewDependencies by lazy { ActivityDependencies(this) }

	private val osPermissionChecker by lazy { OsPermissionsChecker(applicationContext) }

	private val activityResultsLauncher = registerResultActivityLauncher()

	override val readPermissionsRequirements by lazy { ApplicationReadPermissionsRequirementsProvider(osPermissionChecker) }

	override val permissionsManager = this

	override val applicationPermissions by lazy {
		ApplicationPermissionsRequests(
			browserViewDependencies.libraryProvider,
			readPermissionsRequirements,
			this,
			osPermissionChecker
		)
	}

	override val folderPermissions by lazy { WritableFoldersProvider(activityResultsLauncher, contentResolver) }

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

	override fun onNewIntent(intent: Intent?) {
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

	override fun viewNowPlaying(libraryId: LibraryId) = coroutineScope.launch {
		hideBottomSheet()

		if (!navController.moveToTop { it is NowPlayingScreen }) {
			navController.navigate(NowPlayingScreen(libraryId))
		}
	}.toPromise()

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

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun BrowserLibraryDestination.Navigate(
	browserViewDependencies: ScopedBrowserViewDependencies,
	connectionStatusViewModel: ConnectionStatusViewModel,
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
			sheetElevation = 16.dp,
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
			Box(modifier = Modifier.padding(paddingValues)) {
				when (this@Navigate) {
					is LibraryScreen -> {
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
							view(libraryId, null)
						}
					}
					is ItemScreen -> {
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
	}
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun LibraryDestination.Navigate(
	browserViewDependencies: ScopedBrowserViewDependencies,
	connectionStatusViewModel: ConnectionStatusViewModel,
	scaffoldState: BottomSheetScaffoldState,
) {
	with(browserViewDependencies) {
		when (this@Navigate) {
			is BrowserLibraryDestination -> this@Navigate.Navigate(
				browserViewDependencies = browserViewDependencies,
				scaffoldState = scaffoldState,
				connectionStatusViewModel = connectionStatusViewModel,
			)
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
@OptIn(ExperimentalMaterialApi::class)
private fun BrowserView(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
	initialDestination: Destination? = null
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
							connectionStatusViewModel,
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
                                )
							}
						}
				}
				is AboutScreen -> {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(systemBarsPadding)
					) {
						AboutView(graphDependencies.applicationNavigation)
					}
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
