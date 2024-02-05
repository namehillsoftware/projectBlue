package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable

class ActivePlayer(
    private val playlistPlayer: IPlaylistPlayer,
    volumeManagement: PlaylistVolumeManager
) : IActivePlayer {
	private val observableProxy = Observable.create(playlistPlayer).replay(1).refCount()

    init {
        volumeManagement.managePlayer(playlistPlayer)
    }

    override fun pause(): Promise<*> = playlistPlayer.pause()

    override fun resume(): Promise<PositionedPlayingFile?> = playlistPlayer.resume()

    override fun observe(): Observable<PositionedPlayingFile> = observableProxy

	override fun halt(): Promise<*> = playlistPlayer.haltPlayback()
}
