package com.lasthopesoftware.bluewater.client.playback.file.progress

import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface ReadFileProgress {
	val progress: Promise<Duration>
}
