package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.albumArtistOrArtist
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.baseFileNameAsMp3
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.resources.uri.toURI
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.net.URI

class ExternalContentRepository(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val contentResolver: ContentResolver,
) : HaveExternalContent {
	override fun promiseNewContentUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<URI?> = CancellableProxyPromise { cp ->
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.also(cp::doCancel)
			.eventually { fileProperties ->
				QueuedPromise(MessageWriter {
					if (cp.isCancelled) return@MessageWriter null

					val newSongDetails = ContentValues().apply {
						put(MediaStore.Audio.Media.DISPLAY_NAME, fileProperties.baseFileNameAsMp3)
						put(MediaStore.Audio.Media.ARTIST, fileProperties.albumArtistOrArtist)
						put(MediaStore.Audio.Media.ALBUM, fileProperties[KnownFileProperties.Album])
						put(MediaStore.Audio.Media.IS_PENDING, 1)
					}

					if (cp.isCancelled) null
					else contentResolver
						.insert(MediaCollections.ExternalAudio, newSongDetails)
						?.toURI()
				}, ThreadPools.io)
			}
	}

	override fun markContentAsNotPending(uri: URI): Promise<Unit> = QueuedPromise(MessageWriter{
		contentResolver.update(
			uri.toUri(),
			ContentValues().apply {
				put(MediaStore.Audio.Media.IS_PENDING, 0)
			},
			null,
			null
		)

		Unit
	}, ThreadPools.io)

	override fun removeContent(uri: URI): Promise<Boolean> = QueuedPromise(MessageWriter{
		val deletedRecords = contentResolver.delete(uri.toUri(), null, null)
		deletedRecords > 0
	}, ThreadPools.io)
}
