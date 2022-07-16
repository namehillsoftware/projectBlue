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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListActivity.Companion.startItemListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.FileDetailsLauncher
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.SingleUseTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.RateControlledFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ObserveNowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Light
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise

class ItemListActivity : AppCompatActivity(), Runnable {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ItemListActivity::class.java) }
		private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(2) }

		val key by lazy { magicPropertyBuilder.buildProperty("key") }
		val value by lazy { magicPropertyBuilder.buildProperty("value") }
		val playlistIdKey by lazy { magicPropertyBuilder.buildProperty("playlistId") }

		fun Context.startItemListActivity(item: IItem) {
			if (item is Item) this.startItemListActivity(item)
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

	private val itemListViewModel by buildViewModelLazily {
		ItemListViewModel(
			browserLibraryIdProvider,
			itemProvider,
			messageBus,
			storedItemAccess,
			itemListProvider,
			PlaybackServiceController(this),
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

	private val filePropertiesProvider by lazy {
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

	private val fileListViewModel by buildViewModelLazily {
		FileListViewModel(
			browserLibraryIdProvider,
			fileProvider,
			storedItemAccess,
			PlaybackServiceController(this),
			filePropertiesProvider,
			StringResources(this),
			FileDetailsLauncher(this),
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
					nowPlayingState = LiveNowPlayingLookup.getInstance(),
				)
			}
		}
	}

	public override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this).eventually(response({ run() }, handler))
	}

	override fun run() {
		Promise.whenAll(fileListViewModel.loadItem(item), itemListViewModel.loadItem(item))
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

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)

		item = Item(savedInstanceState.getInt(key), savedInstanceState.getString(value))
		val playlistId = savedInstanceState.getInt(playlistIdKey, -1)
		if (playlistId != -1) {
			item = Item(savedInstanceState.getInt(key), savedInstanceState.getString(value), playlistId)
		}
	}

	override fun onBackPressed() {
		if (itemListViewModel.hideAnyShownMenus()) return
		if (fileListViewModel.hideAnyShownMenus()) return
		super.onBackPressed()
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingState: ObserveNowPlaying,
) {
	val activity = LocalContext.current as? Activity ?: return

	val playingFile by nowPlayingState.nowPlayingState.collectAsState()
	val lazyListState = rememberLazyListState()
	val rowHeight = 60.dp
	val hapticFeedback = LocalHapticFeedback.current

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

						itemListViewModel.hideAnyShownMenus()
						fileListViewModel.hideAnyShownMenus()

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
					fontSize = MaterialTheme.typography.h6.fontSize,
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
	fun TrackHeaderItem(position: Int, fileItemViewModel: SingleUseTrackHeadlineViewModel) {
		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()

		LaunchedEffect(Unit) {
			fileItemViewModel.promiseUpdate().suspend()
		}

		if (!isMenuShown) {
			Box(modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

						itemListViewModel.hideAnyShownMenus()
						fileListViewModel.hideAnyShownMenus()

						fileItemViewModel.showMenu()
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = fileItemViewModel::viewFileDetails
				)
				.height(rowHeight)
				.fillMaxSize()
			) {
				val fileName by fileItemViewModel.title.collectAsState()

				Text(
					text = fileName,
					fontSize = MaterialTheme.typography.h6.fontSize,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					fontWeight = if (playingFile?.serviceFile == fileItemViewModel.serviceFile) FontWeight.Bold else FontWeight.Normal,
					modifier = Modifier
						.padding(12.dp)
						.align(Alignment.CenterStart),
				)
			}
		} else {
			Row(modifier = Modifier
				.height(rowHeight)
				.padding(8.dp)) {
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
						.clickable { fileListViewModel.play(position) }
						.align(Alignment.CenterVertically),
				)
			}
		}
	}

	@Composable
	fun BoxScope.LoadedItemListView() {
		val items by itemListViewModel.items.collectAsState()
		val files by fileListViewModel.filesFlow.collectAsState()

		LazyColumn(
			state = lazyListState,
			modifier = Modifier
				.fillMaxSize()
				.scrollbar(
					lazyListState,
					horizontal = false,
					knobColor = MaterialTheme.colors.onSurface,
					visibleAlpha = .4f,
					knobCornerRadius = 1.dp,
				)
		) {
			item {
				Column(modifier = Modifier.padding(4.dp)) {
					val itemValue by itemListViewModel.itemValue.collectAsState()
					ProvideTextStyle(MaterialTheme.typography.h4) {
						Row(modifier = Modifier
							.padding(top = 8.dp)
							.height(80.dp)
						) {
							Text(
								itemValue,
								maxLines = 2,
								overflow = TextOverflow.Ellipsis,
							)
						}
					}

					Row(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)) {
						Image(
							painter = painterResource(id = R.drawable.av_play),
							contentDescription = stringResource(id = R.string.btn_play),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
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
								.clickable { itemListViewModel.toggleSync() }
								.weight(1f),
						)

						Image(
							painter = painterResource(id = R.drawable.av_shuffle),
							contentDescription = stringResource(id = R.string.btn_shuffle_files),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.clickable {
									fileListViewModel.playShuffled()
								}
						)
					}
				}
			}


			if (items.any()) {
				item {
					Box(modifier = Modifier
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
				Box(modifier = Modifier
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

		val isAnyFileMenuShown by fileListViewModel.isAnyMenuShown.collectAsState()
		val isAnyItemMenuShown by itemListViewModel.isAnyMenuShown.collectAsState()
		val isAnyMenuShown = isAnyFileMenuShown || isAnyItemMenuShown

		if (playingFile != null && !isAnyMenuShown) {
			FloatingActionButton(
				onClick = { NowPlayingActivity.startNowPlayingActivity(activity) },
				backgroundColor = MaterialTheme.colors.primary,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(16.dp)
			) {
				Icon(
					painterResource(id = R.drawable.av_play_white),
					stringResource(id = R.string.title_activity_view_now_playing),
				)
			}
		}
	}

	Column(modifier = Modifier.fillMaxSize()) {
		val isItemsLoaded by itemListViewModel.isLoaded.collectAsState()
		val isFilesLoaded by fileListViewModel.isLoaded.collectAsState()
		val isLoaded = isItemsLoaded && isFilesLoaded

		TopAppBar(
			title = { },
			navigationIcon = {
				Icon(
					Icons.Default.ArrowBack,
					contentDescription = "",
					tint = MaterialTheme.colors.onSecondary,
					modifier = Modifier
						.padding(16.dp)
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = activity::finish
						)
				)
			},
			actions = {
				val headerHidingProgress by derivedStateOf {
					if (lazyListState.firstVisibleItemIndex > 0) 1f
					else lazyListState
						.layoutInfo
						.visibleItemsInfo
						.firstOrNull()
						?.size
						?.toFloat()
						?.let { headerSize ->
							1f - ((headerSize - lazyListState.firstVisibleItemScrollOffset) / headerSize)
						}
						?: 0f
				}

				if (isLoaded && headerHidingProgress > 0f) {
					Image(
						painter = painterResource(id = R.drawable.av_play_white),
						contentDescription = stringResource(id = R.string.btn_play),
						modifier = Modifier
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = {
									fileListViewModel.play()
								}
							)
							.padding(start = 8.dp, end = 8.dp)
							.size(24.dp)
							.alpha(headerHidingProgress),
					)

					Image(
						painter = painterResource(id = R.drawable.av_shuffle_white),
						contentDescription = stringResource(id = R.string.btn_shuffle_files),
						modifier = Modifier
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = {
									fileListViewModel.playShuffled()
								}
							)
							.padding(start = 8.dp, end = 8.dp)
							.size(24.dp)
							.alpha(headerHidingProgress),
					)
				}
			},
			backgroundColor = MaterialTheme.colors.secondary,
			contentColor = MaterialTheme.colors.onSecondary,
		)

		Box(modifier = Modifier.fillMaxSize()) {
			if (isLoaded) LoadedItemListView()
			else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
		}
	}
}

