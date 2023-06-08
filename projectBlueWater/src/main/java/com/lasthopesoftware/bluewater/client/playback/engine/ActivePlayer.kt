package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable

class ActivePlayer(
    private val playlistPlayer: IPlaylistPlayer,
    volumeManagement: PlaylistVolumeManager
) : IActivePlayer, AutoCloseable {
    private val fileChangedObservableConnection: Disposable
    private val observableProxy: ConnectableObservable<PositionedPlayingFile>

    init {
        volumeManagement.managePlayer(playlistPlayer)
        observableProxy = Observable.create(playlistPlayer).replay(1)
        fileChangedObservableConnection = observableProxy.connect()
    }

    override fun pause(): Promise<*> {
        return playlistPlayer.pause()
    }

    override fun resume(): Promise<PositionedPlayingFile?> {
        return playlistPlayer.resume()
    }

    override fun observe(): ConnectableObservable<PositionedPlayingFile> {
        return observableProxy
    }

    override fun close() {
        fileChangedObservableConnection.dispose()
    }
}
