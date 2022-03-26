package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.SelectedConnectionFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.SelectedConnectionRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ControlNowPlayingTopSheetBinding
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.scopedApplicationMessageBus
import com.lasthopesoftware.resources.strings.StringResources

class NowPlayingTopFragment : Fragment() {

	private val messageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(requireContext())) }

	private val applicationMessageBus by lazy { requireContext().scopedApplicationMessageBus() }

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

	private val viewModel by buildActivityViewModelLazily {
		val playbackService = PlaybackServiceController(requireContext())

		val nowPlayingViewModel = buildActivityViewModel {
			NowPlayingScreenViewModel(
				messageBus.value,
				applicationMessageBus,
				InMemoryNowPlayingDisplaySettings,
				playbackService,
			)
		}

		NowPlayingFilePropertiesViewModel(
			messageBus.value,
			applicationMessageBus,
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

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = DataBindingUtil.inflate<ControlNowPlayingTopSheetBinding>(
			inflater,
			R.layout.control_now_playing_top_sheet,
			container,
			false
		)

		with (binding) {
			lifecycleOwner = viewLifecycleOwner
			vm = viewModel

			btnPlay.setOnClickListener { v ->
				if (!viewModel.isScreenControlsVisible.value) return@setOnClickListener
				PlaybackService.play(v.context)
				viewModel.togglePlaying(true)
			}

			btnPause.setOnClickListener { v ->
				if (!viewModel.isScreenControlsVisible.value) return@setOnClickListener
				PlaybackService.pause(v.context)
				viewModel.togglePlaying(false)
			}

			btnNext.setOnClickListener { v ->
				if (viewModel.isScreenControlsVisible.value) PlaybackService.next(v.context)
			}

			btnPrevious.setOnClickListener { v ->
				if (viewModel.isScreenControlsVisible.value) PlaybackService.previous(v.context)
			}

			isScreenKeptOnButton.setOnClickListener { viewModel.toggleScreenOn() }

			rbSongRating.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
				if (fromUser) viewModel.updateRating(rating)
			}

			nowPlayingTopSheet.setOnClickListener { viewModel.showNowPlayingControls() }

			viewNowPlayingListButton.setOnClickListener { viewModel.showDrawer() }

			pbNowPlaying.max = viewModel.fileDuration.value
			pbNowPlaying.progress = viewModel.filePosition.value

			return nowPlayingTopSheet
		}
	}
}
