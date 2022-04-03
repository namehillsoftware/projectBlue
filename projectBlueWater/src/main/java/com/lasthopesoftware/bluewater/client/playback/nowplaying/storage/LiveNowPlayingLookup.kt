package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LiveNowPlayingLookup(
	private val selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage,
	private val applicationMessages: RegisterForApplicationMessages
) : GetNowPlayingState, (ApplicationMessage) -> Unit, ViewModel() {

	private var inner: GetNowPlayingState? = null
	private var trackedFile: PositionedFile? = null
	private var trackedPosition: Long? = null

	init {
		val self = this
		with (applicationMessages) {
			registerForClass(cls<BrowserLibrarySelection.LibraryChosenMessage>(), self)
			registerForClass(cls<TrackPositionUpdate>(), self)
			registerForClass(cls<PlaybackMessage.TrackChanged>(), self)
		}
	}

	override fun onCleared() {
		applicationMessages.unregisterReceiver(this)
	}

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		promiseInner()
			.eventually { it?.promiseNowPlaying().keepPromise() }
			.then { np ->
				np?.apply {
					if (trackedFile != playingFile) {
						trackedPosition = null
						trackedFile = playingFile
					}
					filePosition = trackedPosition ?: filePosition
				}
			}

	private fun updateInner(libraryId: LibraryId) {
		inner = NowPlayingRepository(
			SpecificLibraryProvider(libraryId, libraryProvider),
			libraryStorage)
	}

	private fun promiseInner(): Promise<GetNowPlayingState?> =
		inner?.toPromise() ?: selectedLibraryIdentifierProvider.selectedLibraryId.then {
			it?.also(::updateInner)?.let { inner }
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
