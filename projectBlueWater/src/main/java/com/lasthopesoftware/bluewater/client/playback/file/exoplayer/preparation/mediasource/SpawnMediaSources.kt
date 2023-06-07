package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.net.Uri
import com.google.android.exoplayer2.source.MediaSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

fun interface SpawnMediaSources {
	fun promiseNewMediaSource(libraryId: LibraryId, uri: Uri): Promise<MediaSource>
}
