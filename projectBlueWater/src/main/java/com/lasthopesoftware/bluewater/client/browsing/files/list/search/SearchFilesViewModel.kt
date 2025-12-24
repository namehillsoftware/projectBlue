package com.lasthopesoftware.bluewater.client.browsing.files.list.search

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.list.LoadedLibraryState
import com.lasthopesoftware.bluewater.client.browsing.files.list.ServiceFilesListState
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SearchFilesViewModel(
	private val libraryFiles: ProvideLibraryFiles,
) : ViewModel(), TrackLoadedViewState, ServiceFilesListState, LoadedLibraryState {

	override var loadedLibraryId: LibraryId? = null
		private set

	private var lockedInQuery = ""

	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableFiles = MutableInteractionState(emptyList<ServiceFile>())
	private val mutableIsLibraryIdActive = MutableInteractionState(false)

	override val isLoading = mutableIsLoading.asInteractionState()
	val isLibraryIdActive = mutableIsLibraryIdActive.asInteractionState()
	override val files = mutableFiles.asInteractionState()
	val query = MutableInteractionState(lockedInQuery)

	fun prependFilter(filePropertyFilter: FileProperty) {
		query.value = "[${filePropertyFilter.name}]=${filePropertyFilter.value} ${query.value}"
	}

	fun setActiveLibraryId(libraryId: LibraryId) {
		this.loadedLibraryId = libraryId
		mutableIsLibraryIdActive.value = true
	}

	fun findFiles(): Promise<Unit> {
		lockedInQuery = query.value
		return promiseRefresh()
	}

	fun promiseRefresh(): Promise<Unit> {
		mutableIsLoading.value = true
		return loadedLibraryId
			?.let { l ->
				libraryFiles
					.promiseAudioFiles(l, lockedInQuery)
					.then { f -> mutableFiles.value = f }
			}
			.keepPromise(Unit)
			.must { _ -> mutableIsLoading.value = false }
	}

	fun clearResults(): Boolean {
		var isChanged = false
		if (query.value.isNotEmpty()) {
			query.value = ""
			isChanged = true
		}

		if (lockedInQuery.isNotEmpty()) {
			lockedInQuery = ""
			isChanged = true
		}

		if (mutableFiles.value.isNotEmpty()) {
			mutableFiles.value = emptyList()
			isChanged = true
		}

		return isChanged
	}
}
