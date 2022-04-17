package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.net.Uri
import com.google.android.exoplayer2.source.MediaSource
import com.namehillsoftware.handoff.promises.Promise

fun interface SpawnMediaSources {
	fun promiseNewMediaSource(uri: Uri): Promise<MediaSource>
}
