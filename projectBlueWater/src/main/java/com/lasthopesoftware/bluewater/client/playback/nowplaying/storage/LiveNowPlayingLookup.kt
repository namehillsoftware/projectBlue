package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.app.Application
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageRegistrations
import com.lasthopesoftware.bluewater.shared.messages.application.HaveApplicationMessageRegistrations
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

class LiveNowPlayingLookup private constructor(
	selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage,
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
				libraryRepository,
				libraryRepository,
				ApplicationMessageRegistrations,
			)

			return instance
		}

		fun getInstance(): LiveNowPlayingLookup =
			if (::instance.isInitialized) instance
			else throw IllegalStateException("Instance should be initialized in application root")
	}

	private val mutableNowPlayingState = AtomicReference<PositionedFile?>(null)

	@Volatile
	private var inner: GetNowPlayingState? = null

	@Volatile
	private var trackedPosition: Long? = null

	init {
		selectedLibraryIdentifierProvider.promiseSelectedLibraryId().then { it?.also(::updateInner) }

		registrations.registerReceiver { message: BrowserLibrarySelection.LibraryChosenMessage -> updateInner(message.chosenLibraryId) }
		registrations.registerReceiver { message: TrackPositionUpdate -> trackedPosition = message.filePosition.millis }
		registrations.registerReceiver { message: PlaybackMessage.TrackChanged ->
			trackedPosition = null
			mutableNowPlayingState.set(message.positionedFile)
		}
	}

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		inner
			?.promiseNowPlaying()
			?.then { np ->
				if (mutableNowPlayingState.run { compareAndSet(get(), np?.playingFile) }) {
					trackedPosition = null
				}

				np?.let {
					trackedPosition?.let(it::withFilePosition) ?: it
				}
			}
			.keepPromise()

	private fun updateInner(libraryId: LibraryId) {
		inner = NowPlayingRepository(SpecificLibraryProvider(libraryId, libraryProvider), libraryStorage).apply {
				promiseNowPlaying()
					.then {
						mutableNowPlayingState.set(it?.playingFile)
					}
		}
	}
}
