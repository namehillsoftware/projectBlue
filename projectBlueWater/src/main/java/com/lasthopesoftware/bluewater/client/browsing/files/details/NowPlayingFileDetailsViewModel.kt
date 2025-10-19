package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.observables.asInteractionState
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import io.reactivex.rxjava3.core.Observable

class NowPlayingFileDetailsViewModel(
	private val controlPlayback: ControlPlaybackService,
	private val loadFileDetailsState: LoadFileDetailsState,
	private val fileDetailsState: FileDetailsState,
	private val nowPlayingLookup: GetNowPlayingState,
	messageBusRegistration: RegisterForApplicationMessages,
) :
	ViewModel(),
	NowPlayingFileDetailsState,
	PlayableFileDetailsState,
	FileDetailsState by fileDetailsState,
	ImmediateAction,
	ImmediateResponse<NowPlaying?, Unit>,
	PromisedResponse<Unit, Unit>
{
	private var activePositionedFile: PositionedFile? = null

	private val autoCloseableManager by lazy {
		AutoCloseableManager().also(::addCloseable)
	}

	private val mutableIsRemoving = MutableInteractionState(false)
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsInPosition = MutableInteractionState(false)

	override val isRemoving = mutableIsRemoving.asInteractionState()
	override val isInPosition = mutableIsInPosition.asInteractionState()
	override val isLoading = autoCloseableManager.manage(LiftedInteractionState(
		Observable.combineLatest(listOf(mutableIsLoading.mapNotNull(), fileDetailsState.isLoading.mapNotNull())) { source -> source.any { loading -> loading as Boolean } },
		false)
	).asInteractionState()

	init {
	    addCloseable(messageBusRegistration.registerReceiver { changed: LibraryPlaybackMessage.PlaylistChanged ->
			loadIsInPosition(changed.libraryId)
		})
	}

	fun load(libraryId: LibraryId, positionedFile: PositionedFile): Promise<Unit> {
		mutableIsLoading.value = true
		activePositionedFile = positionedFile
		return loadFileDetailsState
			.load(libraryId, positionedFile.serviceFile)
			.eventually(this)
			.must(this)
	}

	override fun removeFile(): Promise<Unit> {
		val libraryId = activeLibraryId ?: return Unit.toPromise()
		val positionedFile = activePositionedFile ?: return Unit.toPromise()

		mutableIsRemoving.value = true
		return controlPlayback
			.removeFromPlaylistAtPosition(libraryId, positionedFile.playlistPosition)
			.must { _ -> mutableIsRemoving.value = false }
	}

	override fun play() {
		val libraryId = activeLibraryId ?: return
		val positionedFile = activePositionedFile ?: return

		controlPlayback.seekTo(libraryId, positionedFile.playlistPosition)
	}

	override fun act() {
		mutableIsLoading.value = false
	}

	override fun promiseResponse(resolution: Unit): Promise<Unit> {
		val libraryId = activeLibraryId ?: return Unit.toPromise()
		return loadIsInPosition(libraryId)
	}

	override fun respond(nowPlaying: NowPlaying?) {
		val currentLibraryId = activeLibraryId
		if (currentLibraryId == null) {
			mutableIsInPosition.value = false
			return
		}

		val positionedFile = activePositionedFile
		if (positionedFile == null) {
			mutableIsInPosition.value = false
			return
		}
		val (position, serviceFile) = positionedFile

		val nowPlaying = nowPlaying ?: return

		mutableIsInPosition.value = nowPlaying.run {
			currentLibraryId == libraryId && position < playlist.size && playlist[position] == serviceFile
		}
	}

	private fun loadIsInPosition(libraryId: LibraryId): Promise<Unit> {
		if (libraryId != activeLibraryId) return Unit.toPromise()

		mutableIsLoading.value = true
		return nowPlayingLookup.promiseNowPlaying(libraryId).then(this).must(this)
	}
}
