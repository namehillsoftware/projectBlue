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
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ControlNowPlayingTopSheetBinding
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.resources.strings.StringResources

class NowPlayingTopFragment : Fragment() {

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

	private val nowPlayingViewModel by buildActivityViewModelLazily {
		NowPlayingViewModel(
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

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = DataBindingUtil.inflate<ControlNowPlayingTopSheetBinding>(
			inflater,
			R.layout.control_now_playing_top_sheet,
			container,
			true
		)

		binding.vm = nowPlayingViewModel

		return with (binding) {
			btnPlay.setOnClickListener { v ->
				if (!nowPlayingViewModel.isScreenControlsVisible.value) return@setOnClickListener
				PlaybackService.play(v.context)
				nowPlayingViewModel.togglePlaying(true)
			}

			btnPause.setOnClickListener { v ->
				if (!nowPlayingViewModel.isScreenControlsVisible.value) return@setOnClickListener
				PlaybackService.pause(v.context)
				nowPlayingViewModel.togglePlaying(false)
			}

			btnNext.setOnClickListener { v ->
				if (nowPlayingViewModel.isScreenControlsVisible.value) PlaybackService.next(v.context)
			}

			btnPrevious.setOnClickListener { v ->
				if (nowPlayingViewModel.isScreenControlsVisible.value) PlaybackService.previous(v.context)
			}

			repeatButton.setOnClickListener { nowPlayingViewModel.toggleRepeating() }

			isScreenKeptOnButton.setOnClickListener { nowPlayingViewModel.toggleScreenOn() }

			rbSongRating.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
				if (fromUser) nowPlayingViewModel.updateRating(rating)
			}

			nowPlayingTopSheet.setOnClickListener { nowPlayingViewModel.showNowPlayingControls() }

//			viewNowPlayingListButton.setOnClickListener(toggleListClickHandler)

			nowPlayingTopSheet
		}
	}
}
