package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchListViewModel(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val libraryFiles: ProvideLibraryFiles,
	private val controlPlaybackService: ControlPlaybackService,
) : ViewModel() {

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableFiles = MutableStateFlow(emptyList<ServiceFile>())

	val isLoading = mutableIsLoading.asStateFlow()
	val files = mutableFiles.asStateFlow()
	val query = MutableStateFlow("")

	fun findFiles(): Promise<Unit> {
		mutableIsLoading.value = true
		return selectedLibraryId
			.promiseSelectedLibraryId()
			.eventually { maybeLibraryId ->
				maybeLibraryId
					?.let { l ->
						val parameters = SearchFileParameterProvider.getFileListParameters(query.value)
						libraryFiles
							.promiseFiles(l, FileListParameters.Options.None, *parameters)
							.then { f -> mutableFiles.value = f }
					}
					.keepPromise(Unit)
			}
			.must { mutableIsLoading.value = false }
	}

	fun play(position: Int = 0) {
		controlPlaybackService.startPlaylist(files.value, position)
	}

	fun playShuffled() = QueuedPromise(MessageWriter {
		controlPlaybackService.startPlaylist(files.value.shuffled())
	}, ThreadPools.compute)
}
