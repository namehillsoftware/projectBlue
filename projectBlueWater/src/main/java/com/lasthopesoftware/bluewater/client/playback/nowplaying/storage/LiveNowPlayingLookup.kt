package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.updateIfDifferent
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

class LiveNowPlayingLookup(
	selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val inner: GetNowPlayingState,
	registrations: RegisterForApplicationMessages,
) : GetNowPlayingState {
	private val activeLibraryId = AtomicReference<LibraryId?>(null)
	private val activePositionedFile = AtomicReference<PositionedFile?>(null)

	@Volatile
	private var trackedPosition: Long? = null

	init {
		selectedLibraryIdentifierProvider.promiseSelectedLibraryId().then { it -> it?.also(::updateInner) }

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
