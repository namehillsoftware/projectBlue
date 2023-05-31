package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaybackStateForSystem {
	fun restoreFromSavedState(libraryId: LibraryId): Promise<Pair<LibraryId, PositionedProgressedFile?>>
	fun interrupt(): Promise<Unit>
	fun pause(): Promise<Unit>
	fun resume(): Promise<Unit>
}
