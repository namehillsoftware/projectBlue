package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.lasthopesoftware.promises.extensions.toPromise
import org.joda.time.Duration

data class PositionedProgressedFile(
	val playlistPosition: Int,
	val serviceFile: ServiceFile,
	private val currentProgress: Duration
) : ReadFileProgress {
	override val progress = currentProgress.toPromise()
}
