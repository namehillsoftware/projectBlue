package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.StorePlaylists
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.move
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingPlaylistViewModel(
	applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val playbackService: ControlPlaybackService,
	private val playlistStorage: StorePlaylists,
) :
	ViewModel(),
	ControlPlaylistEdits,
	HasEditPlaylistState,
	(LibraryPlaybackMessage.PlaylistChanged) -> Unit
{
	private var activeLibraryId: LibraryId? = null
	private var loadedPlaylistPaths = emptyList<String>()
	private val playlistChangedSubscription = applicationMessages.registerReceiver(this)

	private val isRepeatingState = MutableInteractionState(false)
	private val mutableEditingPlaylistState = MutableInteractionState(false)
	private val nowPlayingListState = MutableInteractionState(emptyList<PositionedFile>())
	private val mutableFilteredPlaylistPaths = MutableInteractionState(emptyList<String>())
	private val mutableIsSavingPlaylistActive = MutableInteractionState(false)
	private val mutableSelectedPlaylistPath = MutableInteractionState("")
	private val mutableIsPlaylistPathValid = MutableInteractionState(false)
	private val mutableIsAutoScrolling = MutableInteractionState(false)
	private val mutableIsClearingPlaylistRequested = MutableInteractionState(false)
	private val mutableIsClearingPlaylistRequestGranted = MutableInteractionState(false)

	val isRepeating = isRepeatingState.asInteractionState()
	val isEditingPlaylistState = mutableEditingPlaylistState.asInteractionState()
	val nowPlayingList = nowPlayingListState.asInteractionState()
	val filteredPlaylistPaths = mutableFilteredPlaylistPaths.asInteractionState()
	val isSavingPlaylistActive = mutableIsSavingPlaylistActive.asInteractionState()
	val selectedPlaylistPath = mutableSelectedPlaylistPath.asInteractionState()
	val isPlaylistPathValid = mutableIsPlaylistPathValid.asInteractionState()
	val isAutoScrolling = mutableIsAutoScrolling.asInteractionState()
	override val isClearingPlaylistRequested = mutableIsClearingPlaylistRequested.asInteractionState()
	override val isClearingPlaylistRequestGranted = mutableIsClearingPlaylistRequestGranted.asInteractionState()

	override val isEditingPlaylist: Boolean
		get() = isEditingPlaylistState.value

	fun initializeView(libraryId: LibraryId): Promise<Unit> {
		activeLibraryId = libraryId
		return updateViewFromRepository().unitResponse()
	}

	fun autoScroll() {
		mutableIsAutoScrolling.value = true
	}

	fun manualScroll() {
		mutableIsAutoScrolling.value = false
	}

	override fun editPlaylist() {
		mutableEditingPlaylistState.value = true
	}

	override fun finishPlaylistEdit() {
		mutableEditingPlaylistState.value = false
		disableSavingPlaylist()
	}

	override fun invoke(message: LibraryPlaybackMessage.PlaylistChanged) {
		if (message.libraryId == activeLibraryId)
			updateViewFromRepository()
	}

	override fun onCleared() {
		playlistChangedSubscription.close()
	}

	fun disableSavingPlaylist() {
		mutableIsSavingPlaylistActive.value = false
	}

	fun enableSavingPlaylist() {
		mutableIsSavingPlaylistActive.value = true
	}

	fun updateSelectedPlaylistPath(updatedPath: String) {
		mutableSelectedPlaylistPath.value = updatedPath
		mutableFilteredPlaylistPaths.value =
			if (updatedPath.isEmpty()) loadedPlaylistPaths
			else loadedPlaylistPaths.filter { it.startsWith(updatedPath, true) }
		mutableIsPlaylistPathValid.value = updatedPath.isNotEmpty()
	}

	fun savePlaylist(): Promise<*> = activeLibraryId?.let { libraryId ->
		playlistStorage.promiseStoredPlaylist(libraryId, selectedPlaylistPath.value, nowPlayingList.value.map { it.serviceFile })
	}.keepPromise().must { _ -> mutableIsSavingPlaylistActive.value = false }

	fun swapFiles(from: Int, to: Int) {
		nowPlayingListState.value = nowPlayingListState.value.toMutableList().move(from, to)
	}

	fun toggleRepeating() {
		with (isRepeatingState) {
			value = !value

			activeLibraryId?.also {
				if (value) playbackService.setRepeating(it)
				else playbackService.setCompleting(it)
			}
		}
	}

	override fun requestPlaylistClearingPermission() {
		mutableIsClearingPlaylistRequested.value = true
	}

	override fun grantPlaylistClearing() {
		mutableIsClearingPlaylistRequestGranted.value = isClearingPlaylistRequested.value
	}

	override fun clearPlaylistIfGranted(): Promise<*> {
		if (isClearingPlaylistRequestGranted.value)
			activeLibraryId?.also(playbackService::clearPlaylist)

		mutableIsClearingPlaylistRequested.value = false
		mutableIsClearingPlaylistRequestGranted.value = false
		return Unit.toPromise()
	}

	private fun updateViewFromRepository() =
		activeLibraryId
			?.let {
				val promisedNowPlayingPlaylistUpdate = nowPlayingRepository.promiseNowPlaying(it)
					.then { np ->
						nowPlayingListState.value = np?.positionedPlaylist ?: emptyList()
						isRepeatingState.value = np?.isRepeating ?: false
					}

				val promisedPlaylistPathsUpdate = playlistStorage
					.promiseAudioPlaylistPaths(it)
					.then { paths ->
						loadedPlaylistPaths = paths
						mutableFilteredPlaylistPaths.value = paths
					}

				Promise.whenAll(promisedNowPlayingPlaylistUpdate, promisedPlaylistPathsUpdate)
			}
			.keepPromise()
}
