package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toDeferred
import com.lasthopesoftware.resources.strings.StringResources
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NowPlayingPlaylistFragment : Fragment() {

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val messageBus by lazy { MessageBus(LocalBroadcastManager.getInstance(requireContext())) }

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

	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(messageBus) }

	private val handler by lazy { Handler(requireContext().mainLooper) }

	private val typedMessageBus = lazy { TypedMessageBus<NowPlayingPlaylistMessage>(handler) }

	private val nowPlayingListAdapter by lazy {
		nowPlayingRepository.eventually(LoopedInPromise.response({ r ->
			val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
				r,
				fileListItemNowPlayingRegistrar.value,
				typedMessageBus.value)

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

	private val viewModel by buildActivityViewModelLazily {
		val playbackService = PlaybackServiceController(requireContext())

		val nowPlayingViewModel = buildActivityViewModel {
			NowPlayingScreenViewModel(
				messageBus,
				InMemoryNowPlayingDisplaySettings,
				playbackService,
			)
		}

		NowPlayingFilePropertiesViewModel(
			messageBus,
			LiveNowPlayingLookup.getInstance(),
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
			messageBus,
			LiveNowPlayingLookup.getInstance(),
			typedMessageBus.value
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

				playlistViewModel.nowPlayingList
					.onEach { a.updateListEventually(it).toDeferred().await() }
					.launchIn(lifecycleScope)
			}, requireContext()))

			editNowPlayingList.setOnClickListener { typedMessageBus.value.sendMessage(EditPlaylist) }

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
		if (typedMessageBus.isInitialized()) typedMessageBus.value.close()

		super.onDestroy()
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}
}
