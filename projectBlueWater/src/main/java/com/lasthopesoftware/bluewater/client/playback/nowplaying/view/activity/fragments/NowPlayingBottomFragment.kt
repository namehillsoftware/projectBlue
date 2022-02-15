package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.SelectedConnectionFilePropertiesStorage
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
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.list.NowPlayingFileListAdapter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ControlNowPlayingBottomSheetBinding
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.strings.StringResources
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NowPlayingBottomFragment : Fragment() {

	private val messageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(requireContext())) }

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

	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(messageBus.value) }

	private val nowPlayingListAdapter by lazy {
		nowPlayingRepository.eventually(LoopedInPromise.response({ r ->
			val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
				r,
				fileListItemNowPlayingRegistrar.value)

//			nowPlayingFileListMenuBuilder.setOnViewChangedListener(
//				ViewChangedHandler()
//					.setOnViewChangedListener(this)
//					.setOnAnyMenuShown(this)
//					.setOnAllMenusHidden(this))

			NowPlayingFileListAdapter(requireContext(), nowPlayingFileListMenuBuilder)
		}, requireContext()))
	}

	private val filePropertiesViewModel by buildActivityViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			messageBus.value,
			LiveNowPlayingLookup.getInstance(),
			selectedConnectionProvider,
			lazyFilePropertiesProvider,
			filePropertiesStorage,
			lazySelectedConnectionAuthenticationChecker,
			PlaybackServiceController(requireContext()),
			ConnectionPoller(requireContext()),
			StringResources(requireContext()),
			InMemoryNowPlayingDisplaySettings
		)
	}

	private val nowPlayingViewModel by buildActivityViewModelLazily {
		NowPlayingViewModel(
			messageBus.value,
			InMemoryNowPlayingDisplaySettings
		)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = DataBindingUtil.inflate<ControlNowPlayingBottomSheetBinding>(
			inflater,
			R.layout.control_now_playing_bottom_sheet,
			container,
			false
		)

		binding.vm = filePropertiesViewModel
		binding.lifecycleOwner = viewLifecycleOwner

		nowPlayingListAdapter.eventually(LoopedInPromise.response({ a ->
			val listView = binding.nowPlayingListView
			listView.adapter = a
			listView.layoutManager = LinearLayoutManager(requireContext())

			filePropertiesViewModel.nowPlayingList
				.onEach(a::updateListEventually)
				.launchIn(lifecycleScope)
		}, requireContext()))

		with (binding) {
			miniPlay.setOnClickListener { v ->
				if (!filePropertiesViewModel.isScreenControlsVisible.value) return@setOnClickListener
				PlaybackService.play(v.context)
				filePropertiesViewModel.togglePlaying(true)
			}

			miniPause.setOnClickListener { v ->
				if (!filePropertiesViewModel.isScreenControlsVisible.value) return@setOnClickListener
				PlaybackService.pause(v.context)
				filePropertiesViewModel.togglePlaying(false)
			}

			miniSongRating.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
				if (fromUser) filePropertiesViewModel.updateRating(rating)
			}

			filePropertiesViewModel.nowPlayingFile
				.filterNotNull()
				.onEach {
					if (!nowPlayingViewModel.isDrawerShown.value)
						nowPlayingListView.scrollToPosition(it.playlistPosition)
				}
				.launchIn(lifecycleScope)

			closeNowPlayingList.setOnClickListener { nowPlayingViewModel.hideDrawer() }
		}

		return binding.nowPlayingBottomSheet
	}

	override fun onDestroy() {
		if (fileListItemNowPlayingRegistrar.isInitialized()) fileListItemNowPlayingRegistrar.value.clear()

		super.onDestroy()
	}
}
