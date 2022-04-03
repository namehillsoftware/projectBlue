package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.SelectedConnectionFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.SelectedConnectionRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.list.NowPlayingFileListAdapter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ControlNowPlayingPlaylistBinding
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.ScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toDeferred
import com.lasthopesoftware.resources.strings.StringResources
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*


class NowPlayingPlaylistFragment : Fragment() {

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val applicationMessageBus by lazy { requireContext().getApplicationMessageBus().getScopedMessageBus() }

	private val selectedConnectionProvider by lazy { SelectedConnectionProvider(requireContext()) }

	private val sessionRevisionProvider by lazy { SelectedConnectionRevisionProvider(selectedConnectionProvider) }

	private val lazyFilePropertiesProvider by lazy {
		SelectedConnectionFilePropertiesProvider(selectedConnectionProvider) { c ->
			ScopedFilePropertiesProvider(
				c,
				sessionRevisionProvider,
				FilePropertyCache.getInstance()
			)
		}
	}

	private val lazySelectedConnectionAuthenticationChecker by lazy {
		SelectedConnectionAuthenticationChecker(
			selectedConnectionProvider,
			::ScopedConnectionAuthenticationChecker
		)
	}

	private val filePropertiesStorage by lazy {
		SelectedConnectionFilePropertiesStorage(selectedConnectionProvider) { c ->
			ScopedFilePropertiesStorage(
				c,
				lazySelectedConnectionAuthenticationChecker,
				sessionRevisionProvider,
				FilePropertyCache.getInstance())
		}
	}

	private val selectedLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(requireContext().getApplicationSettingsRepository()) }

	private val nowPlayingRepository by lazy {
		val libraryRepository = LibraryRepository(requireContext())
		selectedLibraryIdProvider.selectedLibraryId
			.then { l ->
				NowPlayingRepository(
					SpecificLibraryProvider(l!!, libraryRepository),
					libraryRepository
				)
			}
	}

	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(applicationMessageBus) }

	private val handler by lazy { Handler(requireContext().mainLooper) }

	private val viewModelMessageBus by buildActivityViewModelLazily { ViewModelMessageBus<NowPlayingPlaylistMessage>(handler) }

	private val scopedMessageReceiver = lazy { ScopedMessageBus(viewModelMessageBus, viewModelMessageBus) }

	private val nowPlayingListAdapter by lazy {
		nowPlayingRepository.eventually(LoopedInPromise.response({ r ->
			val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
				r,
				fileListItemNowPlayingRegistrar.value,
				playlistViewModel,
				scopedMessageReceiver.value,
				viewModelMessageBus)

			itemListMenuChangeHandler?.apply {
				nowPlayingFileListMenuBuilder.setOnViewChangedListener(
					ViewChangedHandler()
						.setOnViewChangedListener(this)
						.setOnAnyMenuShown(this)
						.setOnAllMenusHidden(this)
				)
			}

			NowPlayingFileListAdapter(requireContext(), nowPlayingFileListMenuBuilder)
		}, requireContext()))
	}

	private val liveNowPlayingLookup by buildActivityViewModelLazily {
		val context = requireContext()
		val libraryRepository = LibraryRepository(context)
		LiveNowPlayingLookup(
			SelectedBrowserLibraryIdentifierProvider(context.getApplicationSettingsRepository()),
			libraryRepository,
			libraryRepository,
			applicationMessageBus
		)
	}

	private val viewModel by buildActivityViewModelLazily {
		val playbackService = PlaybackServiceController(requireContext())

		val nowPlayingViewModel = buildActivityViewModel {
			NowPlayingScreenViewModel(
				applicationMessageBus,
				InMemoryNowPlayingDisplaySettings,
				playbackService,
			)
		}

		NowPlayingFilePropertiesViewModel(
            applicationMessageBus,
            liveNowPlayingLookup,
            selectedConnectionProvider,
            lazyFilePropertiesProvider,
            filePropertiesStorage,
            lazySelectedConnectionAuthenticationChecker,
            playbackService,
            ConnectionPoller(requireContext()),
            StringResources(requireContext()),
            nowPlayingViewModel,
            nowPlayingViewModel
        )
	}

	private val playlistViewModel by buildActivityViewModelLazily {
		NowPlayingPlaylistViewModel(
			applicationMessageBus,
			liveNowPlayingLookup,
			viewModelMessageBus
		)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = DataBindingUtil.inflate<ControlNowPlayingPlaylistBinding>(
			inflater,
			R.layout.control_now_playing_playlist,
			container,
			false
		)

		with (binding) {
			lifecycleOwner = viewLifecycleOwner
			vm = viewModel
			playlistVm = playlistViewModel

			nowPlayingListAdapter.eventually(LoopedInPromise.response({ a ->
				val listView = nowPlayingListView
				listView.adapter = a
				listView.layoutManager = LinearLayoutManager(requireContext())
				listView.isNestedScrollingEnabled = true
				listView.itemAnimator = InanimateChangesItemAnimator()

				val dragCallback = NowPlayingDragCallback(requireContext(), a, playlistViewModel)
				val itemTouchHelper = ItemDraggedTouchHelper(dragCallback)
				itemTouchHelper.attachToRecyclerView(listView)
				scopedMessageReceiver.value.registerReceiver(dragCallback)
				scopedMessageReceiver.value.registerReceiver(itemTouchHelper)

				playlistViewModel.nowPlayingList
					.onEach {
						val mutableList = it.toMutableList()
						dragCallback.positionedFiles = mutableList
						a.updateListEventually(mutableList).toDeferred().await()
					}
					.launchIn(lifecycleScope)
			}, requireContext()))

			editNowPlayingList.setOnClickListener { playlistViewModel.editPlaylist() }
			finishEditNowPlayingList.setOnClickListener { playlistViewModel.finishPlaylistEdit() }

			miniPlay.setOnClickListener { v ->
				PlaybackService.play(v.context)
				viewModel.togglePlaying(true)
			}

			miniPause.setOnClickListener { v ->
				PlaybackService.pause(v.context)
				viewModel.togglePlaying(false)
			}

			repeatButton.setOnClickListener { viewModel.toggleRepeating() }

			viewModel.nowPlayingFile
				.filterNotNull()
				.onEach {
					if (!viewModel.isDrawerShown)
						nowPlayingListView.scrollToPosition(it.playlistPosition)
				}
				.launchIn(lifecycleScope)

			closeNowPlayingList.setOnClickListener { viewModel.hideDrawer() }

			miniNowPlayingBar.max = viewModel.fileDuration.value
			miniNowPlayingBar.progress = viewModel.filePosition.value

			return nowPlayingBottomSheet
		}
	}

	override fun onDestroy() {
		if (fileListItemNowPlayingRegistrar.isInitialized()) fileListItemNowPlayingRegistrar.value.clear()
		if (scopedMessageReceiver.isInitialized()) scopedMessageReceiver.value.close()

		super.onDestroy()
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}

	private class NowPlayingDragCallback<ViewHolder : RecyclerView.ViewHolder?>(
		private val context: Context,
		private val adapter: DeferredListAdapter<PositionedFile, ViewHolder>,
		private val playlistState: HasEditPlaylistState,
	) : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.UP or ItemTouchHelper.DOWN,
		0
	), (ItemDragged) -> Unit
	{
		var dragDestination: Int? = null
		var draggedFile: PositionedFile? = null
		var positionedFiles: MutableList<PositionedFile>? = null

		override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
			if (viewHolder.itemViewType != target.itemViewType) return false
			if (!playlistState.isEditingPlaylist) return false

			val dragFrom = viewHolder.adapterPosition

			val dragTo = target.adapterPosition
			if (dragFrom == dragTo) return false

			dragDestination = dragTo
			positionedFiles?.also {
				Collections.swap(it, dragFrom, dragTo)
				adapter.notifyItemMoved(dragFrom, dragTo)
			} ?: return false
			return true
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)

			val dragFrom = draggedFile?.also { draggedFile = null }?.playlistPosition ?: return
			val dragTo = dragDestination ?: return

			// Commit changes
			PlaybackService.moveFile(context, dragFrom, dragTo)
		}

		override fun isLongPressDragEnabled(): Boolean = false

		@Synchronized
		override fun invoke(itemDragged: ItemDragged) {
			draggedFile = itemDragged.positionedFile
		}
	}

	private class ItemDraggedTouchHelper(callback: Callback) : ItemTouchHelper(callback), (ItemDragged) -> Unit {
		override fun invoke(itemDragged: ItemDragged) {
			startDrag(itemDragged.viewHolder)
		}
	}

	private class InanimateChangesItemAnimator : DefaultItemAnimator() {
		override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
			dispatchAddFinished(holder)
			return false
		}
	}
}
