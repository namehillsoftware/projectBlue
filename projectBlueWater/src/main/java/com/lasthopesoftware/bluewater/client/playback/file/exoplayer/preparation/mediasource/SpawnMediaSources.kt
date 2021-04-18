package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.net.Uri
import com.google.android.exoplayer2.source.MediaSource

fun interface SpawnMediaSources {
	fun getNewMediaSource(uri: Uri): MediaSource
}
