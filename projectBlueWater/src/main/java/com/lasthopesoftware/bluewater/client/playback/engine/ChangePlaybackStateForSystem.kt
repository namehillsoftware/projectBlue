package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaybackStateForSystem {
	fun restoreFromSavedState(): Promise<PositionedProgressedFile?>
	fun interrupt(): Promise<Unit>
	fun pause(): Promise<Unit>
	fun resume(): Promise<Unit>
}
