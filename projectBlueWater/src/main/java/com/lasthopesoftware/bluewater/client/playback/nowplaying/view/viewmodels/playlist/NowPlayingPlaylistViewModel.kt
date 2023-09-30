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
import com.lasthopesoftware.bluewater.shared.observables.MutableStateObservable
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
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

	private val isRepeatingState = MutableStateObservable(false)
	private val mutableEditingPlaylistState = MutableStateObservable(false)
	private val nowPlayingListState = MutableStateObservable(emptyList<PositionedFile>())
	private val mutableFilteredPlaylistPaths = MutableStateObservable(emptyList<String>())
	private val mutableIsSavingPlaylistActive = MutableStateObservable(false)
	private val mutableSelectedPlaylistPath = MutableStateObservable("")
	private val mutableIsPlaylistPathValid = MutableStateObservable(false)

	val isRepeating = isRepeatingState.asReadOnly()
	val isEditingPlaylistState = mutableEditingPlaylistState.asReadOnly()
	val nowPlayingList = nowPlayingListState.asReadOnly()
	val filteredPlaylistPaths = mutableFilteredPlaylistPaths.asReadOnly()
	val isSavingPlaylistActive = mutableIsSavingPlaylistActive.asReadOnly()
	val selectedPlaylistPath = mutableSelectedPlaylistPath.asReadOnly()
	val isPlaylistPathValid = mutableIsPlaylistPathValid.asReadOnly()

	fun initializeView(libraryId: LibraryId): Promise<Unit> {
		activeLibraryId = libraryId
		return updateViewFromRepository().unitResponse()
	}

	override val isEditingPlaylist: Boolean
		get() = isEditingPlaylistState.value

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
	}.keepPromise().must {
		mutableIsSavingPlaylistActive.value = false
	}

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
