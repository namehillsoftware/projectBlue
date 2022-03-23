package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackChange
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LiveNowPlayingLookup private constructor(
	selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage
) : GetNowPlayingState, (ApplicationMessage) -> Unit {

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
				with (context.getApplicationMessageBus()) {
					registerForClass(
						cls<BrowserLibrarySelection.LibraryChosenMessage>(),
						liveNowPlayingLookup
					)
					registerForClass(
						cls<TrackPositionUpdate>(),
						liveNowPlayingLookup
					)
					registerForClass(
						cls<PlaylistTrackChange>(),
						liveNowPlayingLookup
					)
				}
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

	private fun updateInner(libraryId: LibraryId) {
		inner = NowPlayingRepository(
			SpecificLibraryProvider(libraryId, libraryProvider),
			libraryStorage
		)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is BrowserLibrarySelection.LibraryChosenMessage -> updateInner(message.chosenLibraryId)
			is TrackPositionUpdate -> trackedPosition = message.filePosition.millis
			is PlaylistTrackChange -> trackedPosition = null
		}
	}
}
