package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.FileDetailsLauncher
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
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Light
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.strings.StringResources

class FileListActivity :
	AppCompatActivity(),
	Runnable {

	companion object {
		private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(1) }

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<FileListActivity>()) }
		private val key by lazy { magicPropertyBuilder.buildProperty("key") }
		private val value by lazy { magicPropertyBuilder.buildProperty("value") }
		private val playlistIdKey by lazy { magicPropertyBuilder.buildProperty("playlistId") }

		@JvmStatic
		fun startFileListActivity(context: Context, item: IItem) {
			if (item is Item) startFileListActivity(context, item)
			else context.startActivity(getFileListIntent(context, item))
		}

		fun startFileListActivity(context: Context, item: Item) {
			val fileListIntent = getFileListIntent(context, item).apply {
				item.playlistId?.also { putExtra(playlistIdKey, it.id) }
			}
			context.startActivity(fileListIntent)
		}

		private fun getFileListIntent(context: Context, item: IItem) = Intent(context, cls<FileListActivity>()).apply {
			putExtra(key, item.key)
			putExtra(value, item.value)
		}
	}

	private val handler by lazy { Handler(mainLooper) }

	private val selectedLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()) }

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

	private val viewModel by lazy {
		fromActiveLibrary(this)
			.then { nowPlayingProvider ->
				nowPlayingProvider
					?.let {
						buildViewModel {
							val applicationMessages = getApplicationMessageBus()
							FileListViewModel(
								applicationMessages,
								selectedLibraryIdProvider,
								fileProvider,
								it,
								StateChangeBroadcastingStoredItemAccess(StoredItemAccess(this), applicationMessages),
								PlaybackServiceController(this),
							)
						}
					}
			}
	}

	private val trackHeadlineViewModelProvider by buildViewModelLazily {
		TrackHeadlineViewModelProvider(
			filePropertiesProvider,
			StringResources(this),
			PlaybackServiceController(this),
			FileDetailsLauncher(this),
		)
	}

	private lateinit var item: Item

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		viewModel
			.then { vm ->
				setContent {
					ProjectBlueTheme {
						FileListView(vm, trackHeadlineViewModelProvider)
					}
				}
			}

		item = Item(
			savedInstanceState?.getInt(key) ?: intent.getIntExtra(key, 1),
			savedInstanceState?.getString(value) ?: intent.getStringExtra(value)
		)

		val playlistId = savedInstanceState?.getInt(playlistIdKey, -1) ?: intent.getIntExtra(playlistIdKey, -1)
		if (playlistId != -1) {
			item = Item(item.key, item.value, playlistId)
		}

		title = "Files"
	}

	override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this).eventually(LoopedInPromise.response({ run() }, handler))
	}

	override fun run() {
		viewModel
			.then { it.loadItem(item) }
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), handler))
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListView(
	viewModel: FileListViewModel,
	trackHeadlineViewModelProvider: TrackHeadlineViewModelProvider,

) {
	val activity = LocalContext.current as? Activity ?: return

	val isSynced by viewModel.isSynced.collectAsState()
	val lazyListState = rememberLazyListState()
	val playingFile by viewModel.playingFile.collectAsState()

	@Composable
	fun TrackHeaderItem(position: Int, serviceFile: ServiceFile) {
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)
		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()

		DisposableEffect(Unit) {
			fileItemViewModel.promiseUpdate(serviceFile)

			onDispose {
				fileItemViewModel.close()
			}
		}

		Box(modifier = Modifier
			.combinedClickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onLongClick = fileItemViewModel::toggleMenu,
				onClickLabel = stringResource(id = R.string.btn_view_song_details),
				onClick = fileItemViewModel::viewFileDetails
			)
			.fillMaxSize()
		) {
			if (!isMenuShown) {
				val fileName by fileItemViewModel.title.collectAsState()

				Text(
					text = fileName,
					fontSize = MaterialTheme.typography.h6.fontSize,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					fontWeight = if (playingFile == serviceFile) FontWeight.Bold else FontWeight.Normal,
					modifier = Modifier.padding(12.dp),
				)
			} else {
				Row(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)) {
					Image(
						painter = painterResource(id = R.drawable.ic_add_item_36dp),
						contentDescription = stringResource(id = R.string.btn_add_file),
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f)
							.clickable {
								fileItemViewModel.addToNowPlaying()
							}
					)

					Image(
						painter = painterResource(id = R.drawable.ic_menu_36dp),
						contentDescription = stringResource(id = R.string.btn_view_files),
						colorFilter = ColorFilter.tint(if (isSynced) MaterialTheme.colors.primary else Light.GrayClickable),
						alpha = if (isSynced) .9f else .6f,
						modifier = Modifier
							.fillMaxWidth()
							.clickable { fileItemViewModel.viewFileDetails() }
							.weight(1f),
					)

					Image(
						painter = painterResource(id = R.drawable.av_play),
						contentDescription = stringResource(id = R.string.btn_play),
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f)
							.clickable { viewModel.play(position) }
					)
				}
			}
		}
	}

	@Composable
	fun BoxScope.LoadedFileListView() {
		val files by viewModel.filesFlow.collectAsState()

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
					val itemValue by viewModel.itemValue.collectAsState()
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

					ProvideTextStyle(MaterialTheme.typography.h6) {
						Text(
							text = "${files.size} files",
							modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
						)
					}

					Row(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)) {
						Image(
							painter = painterResource(id = R.drawable.av_play),
							contentDescription = stringResource(id = R.string.btn_play),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.clickable {
									viewModel.play()
								}
						)

						Image(
							painter = painterResource(id = R.drawable.ic_sync_white),
							contentDescription = stringResource(id = R.string.btn_sync_item),
							colorFilter = ColorFilter.tint(if (isSynced) MaterialTheme.colors.primary else Light.GrayClickable),
							alpha = if (isSynced) .9f else .6f,
							modifier = Modifier
								.fillMaxWidth()
								.clickable { viewModel.toggleSync() }
								.weight(1f),
						)

						Image(
							painter = painterResource(id = R.drawable.av_shuffle),
							contentDescription = stringResource(id = R.string.btn_shuffle_files),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.clickable {
									viewModel.playShuffled()
								}
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

		if (playingFile != null) {
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

	Column(modifier = Modifier.fillMaxSize()) {
		val isLoaded by viewModel.isLoaded.collectAsState()

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
				if (isLoaded && headerHidingProgress > 0f) {
					Image(
						painter = painterResource(id = R.drawable.av_play_white),
						contentDescription = stringResource(id = R.string.btn_play),
						modifier = Modifier
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = {
									viewModel.play()
								}
							)
							.padding(start = 8.dp, end = 8.dp)
							.size(24.dp)
							.alpha(headerHidingProgress),
					)

					Image(
						painter = painterResource(id = R.drawable.ic_sync_white),
						contentDescription = stringResource(id = R.string.btn_sync_item),
						alpha = if (isSynced) 1f else .6f,
						modifier = Modifier
							.padding(start = 8.dp, end = 8.dp)
							.size(24.dp)
							.clickable {
								viewModel.toggleSync()
							}
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
									viewModel.playShuffled()
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
			if (isLoaded) LoadedFileListView()
			else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
		}
	}
}
