package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.observables.ConnectableObservable

interface IActivePlayer {
    fun pause(): Promise<*>
    fun resume(): Promise<PositionedPlayingFile?>
    fun observe(): ConnectableObservable<PositionedPlayingFile>
}
