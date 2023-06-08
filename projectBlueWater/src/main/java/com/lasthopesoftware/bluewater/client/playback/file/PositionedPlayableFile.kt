package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume

class PositionedPlayableFile(
    val playableFile: PlayableFile,
    val playableFileVolumeManager: ManagePlayableFileVolume,
    private val positionedFile: PositionedFile
) : Comparable<PositionedPlayableFile> {
    constructor(
        playlistPosition: Int,
        playbackHandler: PlayableFile,
        playableFileVolumeManager: ManagePlayableFileVolume,
        serviceFile: ServiceFile?
    ) : this(
        playbackHandler,
        playableFileVolumeManager,
        PositionedFile(playlistPosition, serviceFile!!)
    )

    val playlistPosition: Int
        get() = positionedFile.playlistPosition
    val serviceFile: ServiceFile
        get() = positionedFile.serviceFile

    override fun compareTo(other: PositionedPlayableFile): Int {
        return positionedFile.compareTo(other.positionedFile)
    }

    override fun equals(other: Any?): Boolean {
        return other is PositionedPlayableFile && compareTo((other as PositionedPlayableFile?)!!) == 0
    }

    override fun hashCode(): Int {
        return positionedFile.hashCode()
    }

    fun asPositionedFile(): PositionedFile {
        return positionedFile
    }
}
