package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionServiceProxy
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ControlNowPlayingTopSheetBinding
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.resources.strings.StringResources

class NowPlayingTopFragment : Fragment() {

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
            PollConnectionServiceProxy(requireContext()),
            StringResources(requireContext()),
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
			screenState = nowPlayingViewModel

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

			isScreenKeptOnButton.setOnClickListener { nowPlayingViewModel.toggleScreenOn() }

			rbSongRating.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
				if (fromUser) viewModel.updateRating(rating)
			}

			nowPlayingTopSheet.setOnClickListener { viewModel.showNowPlayingControls() }

			viewNowPlayingListButton.setOnClickListener { nowPlayingViewModel.showDrawer() }

			pbNowPlaying.max = viewModel.fileDuration.value
			pbNowPlaying.progress = viewModel.filePosition.value

			return nowPlayingTopSheet
		}
	}
}
