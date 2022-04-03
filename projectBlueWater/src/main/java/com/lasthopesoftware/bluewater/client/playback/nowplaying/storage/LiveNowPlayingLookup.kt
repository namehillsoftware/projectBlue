package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.app.Application
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LiveNowPlayingLookup private constructor(
	selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage
) : GetNowPlayingState, (ApplicationMessage) -> Unit {

	companion object {
		// This needs to be a singleton to ensure the track progress is as up-to-date as possible
		private lateinit var instance: LiveNowPlayingLookup

		@Synchronized
		fun initializeInstance(application: Application): LiveNowPlayingLookup {
			if (::instance.isInitialized) return instance

			val libraryRepository = LibraryRepository(application)
			instance = LiveNowPlayingLookup(
				SelectedBrowserLibraryIdentifierProvider(application.getApplicationSettingsRepository()),
				libraryRepository,
				libraryRepository
			)

			return instance
		}

		fun getInstance(): GetNowPlayingState =
			if (::instance.isInitialized) instance
			else throw IllegalStateException("Instance should be initialized in application root")
	}

	private var inner: GetNowPlayingState? = null
	private var trackedFile: PositionedFile? = null
	private var trackedPosition: Long? = null

	init {
		selectedLibraryIdentifierProvider.selectedLibraryId.then { it?.also(::updateInner) }
	}

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		inner
			?.promiseNowPlaying()
			?.then { np ->
				np?.apply {
					if (trackedFile != playingFile) {
						trackedPosition = null
						trackedFile = playingFile
					}
					filePosition = trackedPosition ?: filePosition
				}
			}
			.keepPromise()

	private fun updateInner(libraryId: LibraryId) {
		inner = NowPlayingRepository(
			SpecificLibraryProvider(libraryId, libraryProvider),
			libraryStorage)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is BrowserLibrarySelection.LibraryChosenMessage -> updateInner(message.chosenLibraryId)
			is TrackPositionUpdate -> trackedPosition = message.filePosition.millis
			is PlaybackMessage.TrackChanged -> {
				trackedPosition = null
				trackedFile = message.positionedFile
			}
		}
	}
}
