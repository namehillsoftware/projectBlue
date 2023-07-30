package com.lasthopesoftware.bluewater.client

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.image.LibraryItemImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableTvChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.TvItemView
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.AboutScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ActiveLibraryDownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ApplicationSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.HiddenSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NewConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SelectedLibraryReRouter
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializingLibrarySelectionNavigation
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.settings.TvLibrarySettingsView
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.TvApplicationSettingsView
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import java.io.IOException

private val logger by lazyLogger<TvActivity>()

class TvActivity :
	AppCompatActivity(),
	PermissionsDependencies,
	ManagePermissions
{
	private val dependencies by lazy { ActivityDependencies(this) }

	private val osPermissionChecker by lazy { OsPermissionsChecker(applicationContext) }
	override val readPermissionsRequirements by lazy { ApplicationReadPermissionsRequirementsProvider(osPermissionChecker) }
	override val writePermissionsRequirements by lazy { ApplicationWritePermissionsRequirementsProvider(osPermissionChecker) }
	override val permissionsManager = this

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				CatalogBrowser(dependencies, this)
			}
		}
	}

	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> = permissions.associateWith { false }.toPromise()
}

private class TvNavigation(
	private val inner: NavigateApplication,
	private val navController: NavController<Destination>
) : NavigateApplication by inner {
	override fun viewApplicationSettings(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }

		return Unit.toPromise()
	}

	override fun viewNewServerSettings(): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(NewConnectionSettingsScreen)

		return Unit.toPromise()
	}

	override fun viewServerSettings(libraryId: LibraryId): Promise<Unit> {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(ConnectionSettingsScreen(libraryId))

		return Unit.toPromise()
	}

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(SearchScreen(libraryId))

		return Unit.toPromise()
	}

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))

		return Unit.toPromise()
	}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> {
		if (item is Item)
			navController.navigate(ItemScreen(libraryId, item))

		return Unit.toPromise()
	}

//	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
//		return inner.viewFileDetails(libraryId, playlist, position)
//	}

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> {
		if (!navController.moveToTop { it is NowPlayingScreen }) {
			navController.navigate(NowPlayingScreen(libraryId))
		}

		return Unit.toPromise()
	}

	override fun backOut(): Promise<Boolean> = navigateUp()

	override fun navigateUp(): Promise<Boolean> {
		return if (navController.pop() && navController.backstack.entries.any()) true.toPromise()
		else inner.navigateUp()
	}
}

private class TvDependencies(
	private val inner: BrowserViewDependencies,
 	private val tvNavigation: TvNavigation,
	private val connectionStatusViewModel: ConnectionStatusViewModel,
) : BrowserViewDependencies by inner, TvInterfaceDependencies {

	override val reusableTvChildItemViewModelProvider by lazy {
		ReusableTvChildItemViewModelProvider(
			LibraryItemImageProvider(RemoteImageAccess(libraryConnectionProvider))
		)
	}

	override val applicationNavigation by lazy {
		ConnectionInitializingLibrarySelectionNavigation(
			tvNavigation,
			selectedLibraryViewModel,
			connectionStatusViewModel,
		)
	}
}

@Composable
fun CatalogBrowser(
	browserViewDependencies: BrowserViewDependencies,
	permissionsDependencies: PermissionsDependencies,
) {
	val navController = rememberNavController(
		listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
	)

	val graphNavigation = remember {
		TvNavigation(browserViewDependencies.applicationNavigation, navController)
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

	val tvDependencies = remember { TvDependencies(browserViewDependencies, graphNavigation, connectionStatusViewModel) }

	BackHandler { tvDependencies.applicationNavigation.backOut() }

	ControlSurface {
		NavHost(navController) { destination ->
			LocalViewModelStoreOwner.current
				?.let {
					ScopedViewModelDependencies(tvDependencies, permissionsDependencies, it)
				}
				?.apply {
					when (destination) {
						AboutScreen -> {}
						ApplicationSettingsScreen -> {
							TvApplicationSettingsView(
								applicationSettingsViewModel,
								applicationNavigation,
								playbackServiceController,
							)

							applicationSettingsViewModel.loadSettings()
						}

						ActiveLibraryDownloadsScreen -> {}
						SelectedLibraryReRouter -> {
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

						HiddenSettingsScreen -> {}
						is DownloadsScreen -> {}
						is ItemScreen -> {
							TvItemView(
								itemListViewModel = itemListViewModel,
								fileListViewModel = fileListViewModel,
								navigateApplication = applicationNavigation,
								tvChildItemViewModelProvider = tvDependencies.reusableTvChildItemViewModelProvider,
								trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
							)

							val (libraryId, item) = destination

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
								TvItemView(
									itemListViewModel = itemListViewModel,
									fileListViewModel = fileListViewModel,
									navigateApplication = applicationNavigation,
									tvChildItemViewModelProvider = tvDependencies.reusableTvChildItemViewModelProvider,
									trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
								)
							}

							if (reinitializeConnection) {
								LaunchedEffect(key1 = Unit) {
									isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
									reinitializeConnection = false
								}
							}

							if (!isConnectionLost) {
								LaunchedEffect(libraryId) {
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

						is LibraryScreen -> {
							val libraryId = destination.libraryId

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
								TvItemView(
									itemListViewModel = itemListViewModel,
									fileListViewModel = fileListViewModel,
									navigateApplication = applicationNavigation,
									tvChildItemViewModelProvider = tvDependencies.reusableTvChildItemViewModelProvider,
									trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
								)
							}

							if (reinitializeConnection) {
								LaunchedEffect(key1 = Unit) {
									isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
									reinitializeConnection = false
								}
							}

							if (!isConnectionLost) {
								LaunchedEffect(libraryId) {
									try {
										Promise.whenAll(
											itemListViewModel.loadItem(libraryId),
											fileListViewModel.loadItem(libraryId),
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

						is SearchScreen -> {}
						is ConnectionSettingsScreen -> {
							TvLibrarySettingsView(
								librarySettingsViewModel = librarySettingsViewModel,
								navigateApplication = applicationNavigation,
								stringResources = stringResources,
							)

							librarySettingsViewModel.loadLibrary(destination.libraryId)
						}
						is NowPlayingScreen -> {}
						NewConnectionSettingsScreen -> {
							TvLibrarySettingsView(
								librarySettingsViewModel = librarySettingsViewModel,
								navigateApplication = applicationNavigation,
								stringResources = stringResources,
							)
						}
					}
				}
		}

		val isCheckingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
		if (isCheckingConnection) {
			Box(
				modifier = Modifier.fillMaxSize()
			) {
				ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
			}
		}
	}
}
