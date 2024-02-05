package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable

interface IActivePlayer {
    fun pause(): Promise<*>
    fun resume(): Promise<PositionedPlayingFile?>
    fun observe(): Observable<PositionedPlayingFile>

	fun halt(): Promise<*>
}
