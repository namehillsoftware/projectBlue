package com.lasthopesoftware.bluewater.client.settings

import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.ManageLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.rxjava3.core.Observable

class ServerTypeSelectionViewModel(
	private val libraryAccess: ManageLibraries,
) : TrackLoadedViewState, ImmediateAction, ImmediateResponse<Library?, Unit> {
	private val loadedLibrary = MutableInteractionState<Library?>(null)
	private val mutableIsLoading = MutableInteractionState(false)

	val serverType = MutableInteractionState(Library.ServerType.MediaCenter)
	val isChanged by lazy {
		LiftedInteractionState(
			Observable.combineLatest(serverType, loadedLibrary) { serverType, library ->
				serverType.value != library.value?.serverType
			},
			false
		)
	}

	override val isLoading = mutableIsLoading.asInteractionState()

	fun promiseLoadedConnectionType(libraryId: LibraryId?): Promise<Unit> {
		mutableIsLoading.value = true
		return libraryId
			?.let(libraryAccess::promiseLibrary)
			?.then(this)
			.keepPromise(Unit)
			.must(this)
	}

	fun promiseSavedConnectionType(): Promise<Unit> {
		val library = loadedLibrary.value?.takeUnless { it.serverType == serverType.value } ?: return Unit.toPromise()

		mutableIsLoading.value = true

		val updatedLibrary = library.copy(
			serverType = serverType.value,
			connectionSettings = null
		)

		return libraryAccess
			.saveLibrary(updatedLibrary)
			.then(this)
			.must(this)
	}

	override fun respond(library: Library?) {
		loadedLibrary.value = library
		serverType.value = library?.serverType ?: Library.ServerType.MediaCenter
	}

	override fun act() {
		mutableIsLoading.value = false
	}
}
