package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.app.Application
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageRegistrations
import com.lasthopesoftware.bluewater.shared.messages.application.HaveApplicationMessageRegistrations
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.updateIfDifferent
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

class LiveNowPlayingLookup private constructor(
	selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val inner: GetNowPlayingState,
	registrations: HaveApplicationMessageRegistrations,
) : GetNowPlayingState {

	companion object {
		// This needs to be a singleton to ensure the track progress is as up-to-date as possible
		private lateinit var instance: LiveNowPlayingLookup

		@Synchronized
		fun initializeInstance(application: Application): LiveNowPlayingLookup {
			if (::instance.isInitialized) return instance

			val libraryRepository = LibraryRepository(application)
			instance = LiveNowPlayingLookup(
				SelectedLibraryIdProvider(application.getApplicationSettingsRepository()),
				NowPlayingRepository(
					application.getCachedSelectedLibraryIdProvider(),
					libraryRepository,
					libraryRepository,
					InMemoryNowPlayingState,
				),
				ApplicationMessageRegistrations,
			)

			return instance
		}

		fun getInstance(): LiveNowPlayingLookup =
			if (::instance.isInitialized) instance
			else throw IllegalStateException("Instance should be initialized in application root")
	}

	private val activeLibraryId = AtomicReference<LibraryId?>(null)
	private val activePositionedFile = AtomicReference<PositionedFile?>(null)

	@Volatile
	private var trackedPosition: Long? = null

	init {
		selectedLibraryIdentifierProvider.promiseSelectedLibraryId().then { it?.also(::updateInner) }

		registrations.registerReceiver { message: BrowserLibrarySelection.LibraryChosenMessage -> updateInner(message.chosenLibraryId) }
		registrations.registerReceiver { message: TrackPositionUpdate -> trackedPosition = message.filePosition.millis }
		registrations.registerReceiver { message: LibraryPlaybackMessage.TrackChanged ->
			trackedPosition = null
			activeLibraryId.updateIfDifferent(message.libraryId)
			activePositionedFile.updateIfDifferent(message.positionedFile)
		}
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> =
		inner
			.promiseActiveNowPlaying()
			.then { np ->
				if (activeLibraryId.updateIfDifferent(np?.libraryId) || activePositionedFile.updateIfDifferent(np?.playingFile)) {
					trackedPosition = null
				}

				np?.let {
					trackedPosition?.let { p -> it.copy(filePosition = p) } ?: it
				}
			}
			.keepPromise()

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> =
		inner
			.promiseNowPlaying(libraryId)
			.then { nowPlaying ->
				nowPlaying
					?.takeIf { activeLibraryId.get() == it.libraryId }
					?.let { activeNowPlaying ->
						if (activePositionedFile.updateIfDifferent(activeNowPlaying.playingFile)) {
							trackedPosition = null
						}

						trackedPosition?.let { p -> activeNowPlaying.copy(filePosition = p) } ?: activeNowPlaying
					}
					?: nowPlaying
			}

	private fun updateInner(libraryId: LibraryId) {
		activeLibraryId.updateIfDifferent(libraryId)
		promiseNowPlaying(libraryId)
	}
}
