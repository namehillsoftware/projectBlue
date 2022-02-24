package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LiveNowPlayingLookup private constructor(
	selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage
) : ReceiveBroadcastEvents, GetNowPlayingState {

	companion object {
		private lateinit var instance: LiveNowPlayingLookup

		@Synchronized
		fun initializeInstance(context: Context) {
			if (::instance.isInitialized) return

			val libraryRepository = LibraryRepository(context)
			instance = LiveNowPlayingLookup(
				SelectedBrowserLibraryIdentifierProvider(context.getApplicationSettingsRepository()),
				libraryRepository,
				libraryRepository
			).also { liveNowPlayingLookup ->
				ApplicationMessageBus(LocalBroadcastManager.getInstance(context)).registerReceiver(
					liveNowPlayingLookup,
					IntentFilter().apply {
						addAction(BrowserLibrarySelection.libraryChosenEvent)
						addAction(TrackPositionBroadcaster.trackPositionUpdate)
						addAction(PlaylistEvents.onPlaylistTrackChange)
					}
				)
			}
		}

		fun getInstance(): LiveNowPlayingLookup =
			if (::instance.isInitialized) instance
			else throw IllegalStateException("Instance should be initialized in application root")
	}

	private var inner: GetNowPlayingState? = null
	private var trackedPosition: Long? = null

	init {
		selectedLibraryIdentifierProvider.selectedLibraryId.then { it?.also(::updateInner) }
	}

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		inner
			?.promiseNowPlaying()
			?.then { np -> np?.apply { filePosition = trackedPosition ?: filePosition } }
			.keepPromise()

	override fun onReceive(intent: Intent) {
		when (intent.action) {
			BrowserLibrarySelection.libraryChosenEvent -> intent
				.getIntExtra(BrowserLibrarySelection.chosenLibraryId, -1)
				.takeIf { it > -1 }
				?.also { libraryId -> updateInner(LibraryId(libraryId)) }
			TrackPositionBroadcaster.trackPositionUpdate -> trackedPosition = intent
				.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
				.takeIf { it > -1 }
			PlaylistEvents.onPlaylistTrackChange -> trackedPosition =null
		}
	}

	private fun updateInner(libraryId: LibraryId) {
		inner = NowPlayingRepository(
			SpecificLibraryProvider(libraryId, libraryProvider),
			libraryStorage
		)
	}
}
