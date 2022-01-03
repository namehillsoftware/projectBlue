package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

data class PositionedProgressedFile(
	val playlistPosition: Int,
	val serviceFile: ServiceFile,
	override val progress: Promise<Duration>
) : ReadFileProgress
