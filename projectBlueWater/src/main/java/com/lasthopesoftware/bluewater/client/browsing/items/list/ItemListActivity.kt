package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsLauncher
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeadlineViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.*
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListActivity.Companion.startItemListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Light
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ExperimentalToolbarApi
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.pow
import kotlin.math.roundToInt

class ItemListActivity : AppCompatActivity(), Runnable {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ItemListActivity::class.java) }

		private val key by lazy { magicPropertyBuilder.buildProperty("key") }
		private val value by lazy { magicPropertyBuilder.buildProperty("value") }
		private val playlistIdKey by lazy { magicPropertyBuilder.buildProperty("playlistId") }

		fun Context.startItemListActivity(item: IItem) {
			if (item is Item) startItemListActivity(item)
			else startActivity(getItemListIntent(this, item))
		}

		fun Context.startItemListActivity(item: Item) {
			val fileListIntent = getItemListIntent(this, item).apply {
				item.playlistId?.also { putExtra(playlistIdKey, it.id) }
			}
			startActivity(fileListIntent)
		}

		private fun getItemListIntent(context: Context, item: IItem) = Intent(context, cls<ItemListActivity>()).apply {
			putExtra(key, item.key)
			putExtra(value, item.value)
		}
	}

	private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(2) }

	private val handler by lazy { Handler(mainLooper) }

	private val itemListProvider by lazy {
		val connectionProvider = ConnectionSessionManager.get(this)

		ItemStringListProvider(
			FileListParameters,
			LibraryFileStringListProvider(connectionProvider)
		)
	}

	private val browserLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()) }

	private val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val messageBus by lazy { getApplicationMessageBus() }

	private val storedItemAccess by lazy {
		StateChangeBroadcastingStoredItemAccess(StoredItemAccess(this), messageBus)
	}

	private val menuMessageBus by buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	private val itemListMenuViewModel by buildViewModelLazily { ItemListMenuViewModel(menuMessageBus) }

	private val itemListViewModel by buildViewModelLazily {
		ItemListViewModel(
			browserLibraryIdProvider,
			itemProvider,
			messageBus,
			storedItemAccess,
			itemListProvider,
			PlaybackServiceController(this),
			menuMessageBus,
		)
	}

	private val fileProvider by lazy {
		val libraryConnectionProvider = ConnectionSessionManager.get(this)
		ItemFileProvider(
			ItemStringListProvider(
				FileListParameters,
				LibraryFileStringListProvider(libraryConnectionProvider)
			)
		)
	}

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache.getInstance(),
			FilePropertiesProvider(
				libraryConnectionProvider,
				revisionProvider,
				FilePropertyCache.getInstance(),
			)
		)
	}

	private val scopedFilePropertiesProvider by lazy {
		SelectedConnectionFilePropertiesProvider(SelectedConnectionProvider(this)) { c ->
			val filePropertyCache = FilePropertyCache.getInstance()
			ScopedCachedFilePropertiesProvider(
				c,
				filePropertyCache,
				RateControlledFilePropertiesProvider(
					ScopedFilePropertiesProvider(
						c,
						ScopedRevisionProvider(c),
						filePropertyCache
					),
					rateLimiter
				)
			)
		}
	}

	private val selectedConnectionAuthenticationChecker by lazy {
		SelectedConnectionAuthenticationChecker(
			selectedConnectionProvider,
			::ScopedConnectionAuthenticationChecker
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
			FilePropertyCache.getInstance(),
			messageBus
		)
	}

	private val fileListViewModel by buildViewModelLazily {
		FileListViewModel(
			browserLibraryIdProvider,
			fileProvider,
			storedItemAccess,
			PlaybackServiceController(this),
		)
	}

	private val selectedConnectionProvider by lazy { SelectedConnectionProvider(this) }

	private val trackHeadlineViewModelProvider by buildViewModelLazily {
		TrackHeadlineViewModelProvider(
			scopedFilePropertiesProvider,
			StringResources(this),
			PlaybackServiceController(this),
			FileDetailsLauncher(this),
			menuMessageBus,
		)
	}

	private val nowPlayingFilePropertiesViewModel by buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			messageBus,
			LiveNowPlayingLookup.getInstance(),
			browserLibraryIdProvider,
			libraryFilePropertiesProvider,
			filePropertiesStorage,
			selectedConnectionAuthenticationChecker,
			PlaybackServiceController(this),
			ConnectionPoller(this),
			StringResources(this),
		)
	}

	private lateinit var item: Item

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		item = Item(
			savedInstanceState?.getInt(key) ?: intent.getIntExtra(key, 1),
			savedInstanceState?.getString(value) ?: intent.getStringExtra(value)
		)

		val playlistId = savedInstanceState?.getInt(playlistIdKey, -1) ?: intent.getIntExtra(playlistIdKey, -1)
		if (playlistId != -1) {
			item = Item(item.key, item.value, playlistId)
		}

		title = item.value

		setContent {
			ProjectBlueTheme {
				ItemListView(
					itemListViewModel = itemListViewModel,
					fileListViewModel = fileListViewModel,
					nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
					itemListMenuViewModel = itemListMenuViewModel,
					trackHeadlineViewModelProvider = trackHeadlineViewModelProvider,
				)
			}
		}

		restoreSelectedConnection(this).eventually(response({ run() }, handler))
	}

	override fun run() {
		Promise.whenAll(fileListViewModel.loadItem(item), itemListViewModel.loadItem(item), nowPlayingFilePropertiesViewModel.initializeViewModel())
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(this), handler))
			.then { finish() }
	}

	override fun onSaveInstanceState(savedInstanceState: Bundle) {
		super.onSaveInstanceState(savedInstanceState)
		savedInstanceState.putInt(key, item.key)
		savedInstanceState.putString(value, item.value)
		item.playlistId?.id?.also {
			savedInstanceState.putInt(playlistIdKey, it)
		}
	}

	override fun onBackPressed() {
		if (!itemListMenuViewModel.hideAllMenus()) super.onBackPressed()
	}
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalToolbarApi::class)
@Composable
private fun ItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuViewModel: ItemListMenuViewModel,
	trackHeadlineViewModelProvider: TrackHeadlineViewModelProvider,
) {
	val activity = LocalContext.current as? Activity ?: return

	val playingFile by nowPlayingViewModel.nowPlayingFile.collectAsState()
	val lazyListState = rememberLazyListState()
	val rowHeight = dimensionResource(id = R.dimen.standard_row_height)
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }
	val hapticFeedback = LocalHapticFeedback.current
	val itemValue by itemListViewModel.itemValue.collectAsState()
	val files by fileListViewModel.files.collectAsState()

	@Composable
	fun ChildItem(childItemViewModel: ItemListViewModel.ChildItemViewModel) {
		val isMenuShown by childItemViewModel.isMenuShown.collectAsState()

		if (!isMenuShown) {
			Box(modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

						itemListMenuViewModel.hideAllMenus()

						childItemViewModel.showMenu()
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = { activity.startItemListActivity(childItemViewModel.item) }
				)
				.height(rowHeight)
				.fillMaxSize()
			) {
				Text(
					text = childItemViewModel.item.value,
					fontSize = rowFontSize,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					fontWeight = FontWeight.Normal,
					modifier = Modifier
						.padding(12.dp)
						.align(Alignment.CenterStart),
				)
			}

			return
		}

		DisposableEffect(Unit) {
			onDispose {
				childItemViewModel.hideMenu()
			}
		}

		Row(modifier = Modifier
			.height(rowHeight)
			.padding(8.dp)) {
			Image(
				painter = painterResource(id = R.drawable.av_play),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable {
						childItemViewModel.play()
					}
					.align(Alignment.CenterVertically),
			)

			val isChildItemSynced by childItemViewModel.isSynced.collectAsState()

			Image(
				painter = painterResource(id = R.drawable.ic_sync_white),
				contentDescription = stringResource(id = R.string.btn_sync_item),
				colorFilter = ColorFilter.tint(if (isChildItemSynced) MaterialTheme.colors.primary else Light.GrayClickable),
				alpha = if (isChildItemSynced) .9f else .6f,
				modifier = Modifier
					.fillMaxWidth()
					.clickable { childItemViewModel.toggleSync() }
					.weight(1f)
					.align(Alignment.CenterVertically),
			)

			Image(
				painter = painterResource(id = R.drawable.av_shuffle),
				contentDescription = stringResource(id = R.string.btn_shuffle_files),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable {
						childItemViewModel.playShuffled()
					}
					.align(Alignment.CenterVertically),
			)
		}
	}

	@Composable
	fun TrackHeaderItem(position: Int, serviceFile: ServiceFile) {
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)
		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()

		DisposableEffect(Unit) {
			fileItemViewModel.promiseUpdate(files, position)

			onDispose {
				fileItemViewModel.reset()
			}
		}

		if (!isMenuShown) {
			Box(modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

						itemListMenuViewModel.hideAllMenus()

						fileItemViewModel.showMenu()
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = fileItemViewModel::viewFileDetails
				)
				.height(rowHeight)
				.fillMaxSize(),
			) {
				val fileName by fileItemViewModel.title.collectAsState()

				Text(
					text = fileName,
					fontSize = rowFontSize,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					fontWeight = if (playingFile?.serviceFile == serviceFile) FontWeight.Bold else FontWeight.Normal,
					modifier = Modifier
						.padding(12.dp)
						.align(Alignment.CenterStart),
				)
			}
		} else {
			Row(modifier = Modifier
				.height(rowHeight)
				.padding(8.dp)
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_add_item_36dp),
					contentDescription = stringResource(id = R.string.btn_add_file),
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f)
						.clickable { fileItemViewModel.addToNowPlaying() }
						.align(Alignment.CenterVertically),
				)

				Image(
					painter = painterResource(id = R.drawable.ic_menu_36dp),
					contentDescription = stringResource(id = R.string.btn_view_files),
					modifier = Modifier
						.fillMaxWidth()
						.clickable { fileItemViewModel.viewFileDetails() }
						.weight(1f)
						.align(Alignment.CenterVertically),
				)

				Image(
					painter = painterResource(id = R.drawable.av_play),
					contentDescription = stringResource(id = R.string.btn_play),
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f)
						.clickable {
							fileItemViewModel.hideMenu()
							fileListViewModel.play(position)
						}
						.align(Alignment.CenterVertically),
				)
			}
		}
	}

	@Composable
	fun BoxWithConstraintsScope.LoadedItemListView() {
		val items by itemListViewModel.items.collectAsState()

		val knobHeight by derivedStateOf {
			lazyListState.layoutInfo.totalItemsCount
				.takeIf { it > 0 }
				?.let { totalItemCount -> maxHeight / (rowHeight * totalItemCount) }
				?.takeIf { it > 0 && it < 1 }
		}

		LazyColumn(
			state = lazyListState,
			modifier = Modifier
				.scrollbar(
					lazyListState,
					horizontal = false,
					knobColor = MaterialTheme.colors.onSurface,
					trackColor = Color.Transparent,
					visibleAlpha = .4f,
					knobCornerRadius = 1.dp,
					fixedKnobRatio = knobHeight,
				)
		) {
			if (items.any()) {
				item {
					Box(
						modifier = Modifier
							.padding(4.dp)
							.height(48.dp)
					) {
						ProvideTextStyle(MaterialTheme.typography.h5) {
							Text(
								text = "${items.size} items",
								fontWeight = FontWeight.Bold,
								modifier = Modifier
									.padding(4.dp)
									.align(Alignment.CenterStart)
							)
						}
					}
				}

				itemsIndexed(items) { i, f ->
					ChildItem(f)

					if (i < items.lastIndex)
						Divider()
				}
			}

			if (!files.any()) return@LazyColumn

			item {
				Box(
					modifier = Modifier
						.padding(4.dp)
						.height(48.dp)
				) {
					ProvideTextStyle(MaterialTheme.typography.h5) {
						Text(
							text = "${files.size} files",
							fontWeight = FontWeight.Bold,
							modifier = Modifier
								.padding(4.dp)
								.align(Alignment.CenterStart)
						)
					}
				}
			}

			itemsIndexed(files) { i, f ->
				TrackHeaderItem(i, f)

				if (i < files.lastIndex)
					Divider()
			}
		}
	}

	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(MaterialTheme.colors.surface)

	Surface {
		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by derivedStateOf { 1 - toolbarState.toolbarState.progress }

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val appBarHeight = 56
				val topPadding by derivedStateOf { (appBarHeight - 46 * headerHidingProgress).dp }
				val expandedTitleHeight = 84
				val expandedIconSize = 36
				val expandedMenuVerticalPadding = 12
				val boxHeight = expandedTitleHeight + expandedIconSize + expandedMenuVerticalPadding * 2 + appBarHeight
				BoxWithConstraints(modifier = Modifier
					.height(boxHeight.dp)
					.padding(top = topPadding)) {
					val minimumMenuWidth = (3 * 32).dp
					val acceleratedProgress by derivedStateOf { 1 - toolbarState.toolbarState.progress.pow(3).coerceIn(0f, 1f) }
					ProvideTextStyle(MaterialTheme.typography.h5) {
						val startPadding by derivedStateOf { (4 + 48 * headerHidingProgress).dp }
						val endPadding by derivedStateOf { 4.dp + minimumMenuWidth * acceleratedProgress }
						val maxLines by derivedStateOf { (2 - headerHidingProgress).roundToInt() }
						if (maxLines > 1) {
							Text(
								text = itemValue,
								maxLines = maxLines,
								overflow = TextOverflow.Ellipsis,
								modifier = Modifier
									.fillMaxWidth()
									.padding(start = startPadding, end = endPadding),
							)
						} else {
							MarqueeText(
								text = itemValue,
								overflow = TextOverflow.Ellipsis,
								gradientSides = setOf(GradientSide.End),
								gradientEdgeColor = MaterialTheme.colors.surface,
								modifier = Modifier
									.fillMaxWidth()
									.padding(start = startPadding, end = endPadding),
							)
						}
					}

					val menuWidth by derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedProgress) }
					val expandedTopRowPadding = expandedTitleHeight + expandedMenuVerticalPadding
					val collapsedTopRowPadding = 6
					val topRowPadding by derivedStateOf { (expandedTopRowPadding - (expandedTopRowPadding - collapsedTopRowPadding) * headerHidingProgress).dp }
					Row(modifier = Modifier
						.padding(top = topRowPadding, bottom = expandedMenuVerticalPadding.dp, start = 8.dp, end = 8.dp)
						.width(menuWidth)
						.align(Alignment.TopEnd)
					) {
						val iconSize by derivedStateOf { (expandedIconSize - (12 * headerHidingProgress)).dp }

						Image(
							painter = painterResource(id = R.drawable.av_play),
							contentDescription = stringResource(id = R.string.btn_play),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.size(iconSize)
								.clickable {
									fileListViewModel.play()
								}
						)

						val isSynced by itemListViewModel.isSynced.collectAsState()

						Image(
							painter = painterResource(id = R.drawable.ic_sync_white),
							contentDescription = stringResource(id = R.string.btn_sync_item),
							colorFilter = ColorFilter.tint(if (isSynced) MaterialTheme.colors.primary else Light.GrayClickable),
							alpha = if (isSynced) .9f else .6f,
							modifier = Modifier
								.fillMaxWidth()
								.size(iconSize)
								.clickable { itemListViewModel.toggleSync() }
								.weight(1f),
						)

						Image(
							painter = painterResource(id = R.drawable.av_shuffle),
							contentDescription = stringResource(id = R.string.btn_shuffle_files),
							modifier = Modifier
								.fillMaxWidth()
								.size(iconSize)
								.weight(1f)
								.clickable {
									fileListViewModel.playShuffled()
								}
						)
					}
				}

				Box(modifier = Modifier.height(appBarHeight.dp)) {
					Icon(
						Icons.Default.ArrowBack,
						contentDescription = "",
						tint = MaterialTheme.colors.onSurface,
						modifier = Modifier
							.padding(16.dp)
							.align(Alignment.CenterStart)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = activity::finish
							)
					)
				}
			},
		) {
			BoxWithConstraints(modifier = Modifier
				.padding(bottom = 56.dp)
				.fillMaxSize()) {
				val isItemsLoaded by itemListViewModel.isLoaded.collectAsState()
				val isFilesLoaded by fileListViewModel.isLoaded.collectAsState()
				val isLoaded = isItemsLoaded && isFilesLoaded

				if (isLoaded) LoadedItemListView()
				else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}

			if (playingFile != null) {
				BottomAppBar(
					backgroundColor = MaterialTheme.colors.secondary,
					contentPadding = PaddingValues(0.dp),
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.clickable { NowPlayingActivity.startNowPlayingActivity(activity) }
				) {
					Column {
						Row(
							modifier = Modifier
								.weight(1f)
								.padding(start = 16.dp, end = 16.dp)
						) {
							Column(
								modifier = Modifier
									.weight(1f)
									.align(Alignment.CenterVertically),
							) {
								val songTitle by nowPlayingViewModel.title.collectAsState()

								ProvideTextStyle(MaterialTheme.typography.subtitle1) {
									Text(
										text = songTitle ?: stringResource(id = R.string.lbl_loading),
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										fontWeight = FontWeight.Medium
									)
								}

								val songArtist by nowPlayingViewModel.artist.collectAsState()
								ProvideTextStyle(MaterialTheme.typography.body2) {
									Text(
										text = songArtist ?: stringResource(id = R.string.lbl_loading),
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
						val fileProgress by derivedStateOf { filePosition / fileDuration.toFloat() }
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
			}
		}
	}
}

