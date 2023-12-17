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
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import org.apache.commons.io.FilenameUtils
import java.net.URI
import java.util.regex.Pattern

class ExternalContentRepository(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val contentResolver: ContentResolver,
) : HaveExternalContent {

	companion object {
		private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

		private fun String.replaceReservedCharsAndPath(): String =
			reservedCharactersPattern.matcher(this).replaceAll("_")
	}

	override fun promiseNewContentUri(externalContent: ExternalContent): Promise<URI?> =
		QueuedPromise(CancellableMessageWriter { ct ->
			externalContent
				.takeUnless { ct.isCancelled }
				?.toContentValues()
				?.takeUnless { ct.isCancelled }
				?.let { newContent ->
					contentResolver.insert(MediaCollections.ExternalAudio, newContent)
				}
				?.toURI()
		}, ThreadPools.io)

	override fun promiseNewContentUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<URI?> = CancellableProxyPromise { cp ->
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.also(cp::doCancel)
			.eventually { fileProperties ->
				promiseNewContentUri(
					ExternalAudioContent(
						displayName = fileProperties.baseFileNameAsMp3,
						artist = fileProperties.albumArtistOrArtist,
						album = fileProperties[KnownFileProperties.Album],
						relativePath = fileProperties
							.albumArtistOrArtist?.trim { c -> c <= ' ' }
							?.replaceReservedCharsAndPath()
							?.let { path ->
								fileProperties[KnownFileProperties.Album]
									?.let { album ->
										FilenameUtils.concat(
											path,
											album.trim { it <= ' ' }.replaceReservedCharsAndPath()
										)
									}
									?: path
							}
					)
				).also(cp::doCancel)
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
