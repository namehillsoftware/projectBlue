package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchFilesViewModel(
	private val libraryFiles: ProvideLibraryFiles,
) : ViewModel(), TrackLoadedViewState {

	private var libraryId: LibraryId? = null

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableFiles = MutableStateFlow(emptyList<ServiceFile>())
	private val mutableIsLibraryIdActive = MutableStateFlow(false)

	override val isLoading = mutableIsLoading.asStateFlow()
	val isLibraryIdActive = mutableIsLibraryIdActive.asStateFlow()
	val files = mutableFiles.asStateFlow()
	val query = MutableStateFlow("")

	fun setActiveLibraryId(libraryId: LibraryId) {
		this.libraryId = libraryId
		mutableIsLibraryIdActive.value = true
	}

	fun findFiles(): Promise<Unit> {
		mutableIsLoading.value = true
		return libraryId
			?.let { l ->
				val parameters = SearchFileParameterProvider.getFileListParameters(query.value)
				libraryFiles
					.promiseFiles(l, FileListParameters.Options.None, *parameters)
					.then { f -> mutableFiles.value = f }
			}
			.keepPromise(Unit)
			.must { mutableIsLoading.value = false }
	}
}
