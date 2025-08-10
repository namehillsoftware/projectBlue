package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.observables.InteractionState
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.namehillsoftware.handoff.promises.Promise

interface FileDetailsState {
	val activeLibraryId: LibraryId?
	val activeServiceFile: ServiceFile?

	val fileName: InteractionState<String>
	val artist: InteractionState<String>
	val album: InteractionState<String>
	val fileProperties: InteractionState<List<FileDetailsViewModel.FilePropertyViewModel>>
	val isLoading: InteractionState<Boolean>
	val coverArt: LiftedInteractionState<ByteArray>
	val rating: InteractionState<Int>
	val highlightedProperty: InteractionState<FileDetailsViewModel.FilePropertyViewModel?>

	fun addToNowPlaying()
	fun promiseLoadedActiveFile(): Promise<Unit>
}
