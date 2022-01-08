package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaybackStateForSystem {
	fun restoreFromSavedState(): Promise<PositionedProgressedFile?>
	fun interruptPlaybackTemporarily(): Promise<Unit>
	fun pause(): Promise<Unit>
}
