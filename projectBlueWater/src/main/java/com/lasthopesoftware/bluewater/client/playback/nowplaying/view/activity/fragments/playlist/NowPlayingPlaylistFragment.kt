package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import android.content.Context
import android.os.Bundle
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
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.InMemoryNowPlayingState
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
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.ScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.strings.StringResources
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*


class NowPlayingPlaylistFragment : Fragment() {

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val applicationMessageBus by lazy { getApplicationMessageBus() }

	private val libraryConnectionProvider by lazy { requireContext().buildNewConnectionSessionManager() }

	private val connectionAuthenticationChecker by lazy {
		ConnectionAuthenticationChecker(libraryConnectionProvider)
	}

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val libraryFilePropertiesProvider by lazy {
		FilePropertiesProvider(
			libraryConnectionProvider,
			revisionProvider,
			FilePropertyCache,
		)
	}

	private val filePropertiesStorage by lazy {
		FilePropertyStorage(
			libraryConnectionProvider,
			connectionAuthenticationChecker,
			revisionProvider,
			FilePropertyCache,
			applicationMessageBus
		)
	}

	private val selectedLibraryIdProvider by lazy { requireContext().getCachedSelectedLibraryIdProvider() }

	private val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			selectedLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider)
		)
	}

	private val nowPlayingRepository by lazy {
		val libraryRepository = LibraryRepository(requireContext())
		selectedLibraryIdProvider.promiseSelectedLibraryId()
			.then { l ->
				NowPlayingRepository(
					SpecificLibraryProvider(l!!, libraryRepository),
					libraryRepository,
					InMemoryNowPlayingState,
				)
			}
	}

	private val viewModelMessageBus by buildActivityViewModelLazily { ViewModelMessageBus<NowPlayingPlaylistMessage>() }

	private val scopedMessageReceiver by lazyScoped { ScopedMessageBus(viewModelMessageBus, viewModelMessageBus) }

	private val nowPlayingListAdapter by lazy {
		nowPlayingRepository.eventually(LoopedInPromise.response({ r ->
			val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
				r,
				applicationMessageBus,
				playlistViewModel,
				scopedMessageReceiver,
				viewModelMessageBus,
				scopedUrlKeyProvider
			)

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

	private val nowPlayingViewModel by buildActivityViewModelLazily {
		NowPlayingScreenViewModel(
			applicationMessageBus,
			InMemoryNowPlayingDisplaySettings,
			PlaybackServiceController(requireContext()),
		)
	}

	private val viewModel by buildActivityViewModelLazily {
		val playbackService = PlaybackServiceController(requireContext())

		NowPlayingFilePropertiesViewModel(
            applicationMessageBus,
            LiveNowPlayingLookup.getInstance(),
            libraryFilePropertiesProvider,
            UrlKeyProvider(libraryConnectionProvider),
            filePropertiesStorage,
            connectionAuthenticationChecker,
            playbackService,
            ConnectionPoller(requireContext()),
            StringResources(requireContext()),
        )
	}

	private val playlistViewModel by buildActivityViewModelLazily {
		NowPlayingPlaylistViewModel(
			applicationMessageBus,
			LiveNowPlayingLookup.getInstance(),
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
				scopedMessageReceiver.registerReceiver(dragCallback)
				scopedMessageReceiver.registerReceiver(itemTouchHelper)

				playlistViewModel.nowPlayingList
					.onEach {
						val mutableList = it.toMutableList()
						dragCallback.positionedFiles = mutableList
						a.updateListEventually(mutableList).suspend()
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
					if (!nowPlayingViewModel.isDrawerShown)
						nowPlayingListView.scrollToPosition(it.playlistPosition)
				}
				.launchIn(lifecycleScope)

			closeNowPlayingList.setOnClickListener { nowPlayingViewModel.hideDrawer() }

			miniNowPlayingBar.max = viewModel.fileDuration.value
			miniNowPlayingBar.progress = viewModel.filePosition.value

			return nowPlayingBottomSheet
		}
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
