package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class PositionedPlayingFile(
	val playingFile: PlayingFile,
	val playableFileVolumeManager: ManagePlayableFileVolume,
	private val positionedFile: PositionedFile
) : Comparable<PositionedPlayingFile>, ReadFileProgress {
	constructor(playlistPosition: Int, playingFile: PlayingFile, playableFileVolumeManager: ManagePlayableFileVolume, serviceFile: ServiceFile)
		: this(playingFile, playableFileVolumeManager, PositionedFile(playlistPosition, serviceFile))

	val playlistPosition: Int
		get() = positionedFile.playlistPosition
	val serviceFile: ServiceFile
		get() = positionedFile.serviceFile
	override val progress: Promise<Duration>
		get() = playingFile.promisePlayedFile().progress

	override fun compareTo(other: PositionedPlayingFile): Int = positionedFile.compareTo(other.positionedFile)

	override fun equals(other: Any?): Boolean =
		(other as? PositionedPlayingFile)?.let { compareTo(it) == 0 } ?: false

	override fun hashCode(): Int = positionedFile.hashCode()

	fun asPositionedFile(): PositionedFile = positionedFile
}
