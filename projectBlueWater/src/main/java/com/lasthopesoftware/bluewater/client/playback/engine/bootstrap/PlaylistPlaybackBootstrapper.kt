package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap

import com.lasthopesoftware.bluewater.client.playback.engine.ActivePlayer
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import org.joda.time.Duration
import java.io.Closeable

class PlaylistPlaybackBootstrapper(private val volumeManagement: PlaylistVolumeManager) : IStartPlayback, Closeable {

    private var playlistPlayer: PlaylistPlayer? = null
    private var activePlayer: ActivePlayer? = null

    override fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Duration): IActivePlayer {
        close()
        playlistPlayer = PlaylistPlayer(preparedPlaybackQueue, filePosition)
        return ActivePlayer(playlistPlayer, volumeManagement).also { activePlayer = it }
    }

    override fun close() {
        activePlayer?.close()
        playlistPlayer?.close()
    }
}
