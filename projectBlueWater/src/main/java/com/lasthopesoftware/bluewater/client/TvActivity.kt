package com.lasthopesoftware.bluewater.client

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.lasthopesoftware.promises.extensions.toPromise
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@UnstableApi
class TvActivity : AppCompatActivity(), ActivitySuppliedDependencies {
	private val dependencies by lazy { ActivityDependencies(this, this) }

	override val registeredActivityResultsLauncher = registerResultActivityLauncher()

	override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onCreate(savedInstanceState, persistentState)

		setContent {
			ProjectBlueTheme {
				CatalogBrowser(dependencies)
			}
		}
	}
}

private class TvNavigation(
	private val navController: NavController<Destination>,
	private val coroutineScope: CoroutineScope
) : NavigateApplication {
	override fun launchSearch(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ItemScreen }

		navController.navigate(SearchScreen(libraryId))
	}.toPromise()

	override fun viewLibrary(libraryId: LibraryId) = coroutineScope.launch {
		navController.popUpTo { it is ApplicationSettingsScreen }

		navController.navigate(LibraryScreen(libraryId))
	}.toPromise()

	override fun viewItem(libraryId: LibraryId, item: IItem) = coroutineScope.launch {
		if (item is Item)
			navController.navigate(ItemScreen(libraryId, item))
	}.toPromise()

//	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
//		return inner.viewFileDetails(libraryId, playlist, position)
//	}

	override fun viewNowPlaying(libraryId: LibraryId) = coroutineScope.launch {
		if (!navController.moveToTop { it is NowPlayingScreen }) {
			navController.navigate(NowPlayingScreen(libraryId))
		}
	}.toPromise()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvItemView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	navigateApplication: NavigateApplication,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
) {
	Column {
		val itemTitle by itemListViewModel.itemValue.collectAsState()

		Text(
			text = itemTitle,
			style = MaterialTheme.typography.headlineMedium,
		)

		val childItems by itemListViewModel.items.collectAsState()
		Text(
			text = stringResource(id = R.string.item_count_label, childItems.size),
			style = MaterialTheme.typography.headlineSmall,
		)

		TvLazyRow(
			horizontalArrangement = Arrangement.spacedBy(Dimensions.viewPaddingUnit * 2)
		) {
			items(childItems) { child ->
				Card(
					onClick = {
						itemListViewModel.loadedLibraryId?.also {
							navigateApplication.viewItem(it, child)
						}
					}
				) {
					Text(text = child.value ?: "")
				}
			}
		}

		val childFiles by fileListViewModel.files.collectAsState()
		Text(
			text = stringResource(id = R.string.file_count_label, childFiles.size),
			style = MaterialTheme.typography.headlineSmall,
		)

		TvLazyRow(
			horizontalArrangement = Arrangement.spacedBy(Dimensions.viewPaddingUnit * 2)
		) {
			itemsIndexed(childFiles) { i, serviceFile ->
				Card(
					onClick = {
						itemListViewModel.loadedLibraryId?.also {
							navigateApplication.viewFileDetails(it, childFiles, i)
						}
					}
				) {
					val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

					DisposableEffect(serviceFile) {
						itemListViewModel.loadedLibraryId?.also {
							fileItemViewModel.promiseUpdate(it, serviceFile)
						}

						onDispose {
							fileItemViewModel.reset()
						}
					}

					val title by fileItemViewModel.title.collectAsState()

					Text(text = title)
				}
			}
		}
	}
}

private class TvDependencies(
	private val inner: BrowserViewDependencies,
 	override val applicationNavigation: NavigateApplication
) : BrowserViewDependencies by inner

@Composable
fun CatalogBrowser(
	browserViewDependencies: BrowserViewDependencies
) {
	val navController = rememberNavController(
		listOf(ApplicationSettingsScreen, SelectedLibraryReRouter)
	)

	val coroutineScope = rememberCoroutineScope()

	val graphNavigation = remember {
		TvNavigation(navController, coroutineScope)
	}

	val tvDependencies = TvDependencies(browserViewDependencies, graphNavigation)

	NavHost(navController) { destination ->
		when (destination) {
			ApplicationSettingsScreen -> {}
			ActiveLibraryDownloadsScreen -> {}
			SelectedLibraryReRouter -> {}
			HiddenSettingsScreen -> {}
			is DownloadsScreen -> {}
			is ItemScreen -> {
				with (tvDependencies) {
					TvItemView(
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
						navigateApplication = applicationNavigation,
						trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					)
				}
			}
			is LibraryScreen -> {}
			is SearchScreen -> {}
			is ConnectionSettingsScreen -> {}
			is NowPlayingScreen -> {}
			NewConnectionSettingsScreen -> {}
		}
	}
}
