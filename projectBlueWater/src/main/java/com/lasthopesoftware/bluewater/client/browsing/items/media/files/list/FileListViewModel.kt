package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ProvideNowPlayingFiles
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileListViewModel(
	applicationMessages: RegisterForApplicationMessages,
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val itemFileProvider: ProvideItemFiles,
	private val nowPlayingFileProvider: ProvideNowPlayingFiles,
	private val storedItemAccess: AccessStoredItems,
	private val controlPlaybackService: ControlPlaybackService,
) : ViewModel() {

	private val playingFileUpdatedRegistration = applicationMessages.registerReceiver { c: PlaybackMessage.TrackChanged ->
		mutablePlayingFile.value = c.positionedFile.serviceFile
	}
	private val mutableIsLoaded = MutableStateFlow(false)
	private val mutableFiles = MutableStateFlow(emptyList<ServiceFile>())
	private val mutablePlayingFile = MutableStateFlow<ServiceFile?>(null)
	private val mutableItemValue = MutableStateFlow("")
	private val mutableIsSynced = MutableStateFlow(false)

	private var loadedItem: IItem? = null
	private var loadedLibraryId: LibraryId? = null

	val isLoaded = mutableIsLoaded.asStateFlow()
	val filesFlow = mutableFiles.asStateFlow()
	val playingFile = mutablePlayingFile.asStateFlow()
	val itemValue = mutableItemValue.asStateFlow()
	val isSynced = mutableIsSynced.asStateFlow()

	override fun onCleared() {
		playingFileUpdatedRegistration.close()
	}

	fun loadItem(item: IItem): Promise<Unit> {
		mutableIsLoaded.value = false
		mutableItemValue.value = item.value

		val promisedNowPlayingFile = nowPlayingFileProvider
			.nowPlayingFile
			.then {
				mutablePlayingFile.value = it
			}

		return selectedLibraryId.selectedLibraryId
			.eventually { libraryId ->
				libraryId
					?.let {
						loadedLibraryId = libraryId
						val promisedFilesUpdate = itemFileProvider
							.promiseFiles(libraryId, ItemId(item.key), FileListParameters.Options.None)
							.then { f ->
								mutableFiles.value = f
							}

						val promisedSyncUpdate = storedItemAccess
							.isItemMarkedForSync(libraryId, item)
							.then { isSynced ->
								mutableIsSynced.value = isSynced
							}

						Promise.whenAll(promisedFilesUpdate, promisedSyncUpdate, promisedNowPlayingFile)
					}
					?: promisedNowPlayingFile
			}
			.then {
				loadedItem = item
				mutableIsLoaded.value = true
			}
	}

	fun toggleSync(): Promise<Unit> = loadedLibraryId
		?.let { libraryId ->
			loadedItem?.let { (it as? Item)?.playlistId ?: ItemId(it.key) }?.let { key ->
				val isSynced = !mutableIsSynced.value
				storedItemAccess
					.toggleSync(libraryId, key, isSynced)
					.then { mutableIsSynced.value = isSynced }
			}
		}
		.keepPromise(Unit)

	fun play(position: Int = 0) {
		controlPlaybackService.startPlaylist(filesFlow.value, position)
	}

	fun playShuffled() = QueuedPromise(MessageWriter {
		controlPlaybackService.startPlaylist(filesFlow.value.shuffled())
	}, ThreadPools.compute)
}
