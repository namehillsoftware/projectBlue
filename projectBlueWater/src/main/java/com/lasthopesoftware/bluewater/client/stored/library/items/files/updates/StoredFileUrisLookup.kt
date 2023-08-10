package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.albumArtistOrArtist
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.baseFileNameAsMp3
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.resources.uri.toURI
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.URI
import java.util.regex.Pattern

class StoredFileUrisLookup(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val libraryProvider: ILibraryProvider,
	private val lookupSyncDirectory: LookupSyncDirectory,
	private val mediaFileUriProvider: MediaFileUriProvider,
	private val contentResolver: ContentResolver
) : GetStoredFileUris {

	companion object {
		private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

		private fun String.replaceReservedCharsAndPath(): String =
			reservedCharactersPattern.matcher(this).replaceAll("_")
	}

	override fun promiseStoredFileUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<URI?> =
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				libraryProvider
					.promiseLibrary(libraryId)
					.eventually { l ->
						when (l?.syncedFileLocation) {
							Library.SyncedFileLocation.INTERNAL -> promiseLocalFileUri(libraryId, fileProperties)
							Library.SyncedFileLocation.EXTERNAL -> promiseContentUri(libraryId, serviceFile, fileProperties)
							else -> Promise.empty()
						}
					}
			}

	private fun promiseLocalFileUri(libraryId: LibraryId, fileProperties: Map<String, String>): Promise<URI?> =
		lookupSyncDirectory
			.promiseSyncDirectory(libraryId)
			.then { syncDir ->
				syncDir
					?.path
					?.let { fullPath ->
						fileProperties
							.albumArtistOrArtist
							?.let { a ->
									FilenameUtils.concat(
										fullPath,
										a.trim { c -> c <= ' ' }.replaceReservedCharsAndPath()
									)
							}
							?: fullPath
					}
					?.let { fullPath ->
						fileProperties[KnownFileProperties.Album]
							?.let { album ->
								FilenameUtils.concat(
									fullPath,
									album.trim { it <= ' ' }.replaceReservedCharsAndPath()
								)
							}
							?: fullPath
					}
					?.let { fullPath ->
						fileProperties.baseFileNameAsMp3
							?.let { fileName ->
								FilenameUtils.concat(fullPath, fileName).trim { it <= ' ' }
							}
							?: fullPath
					}
					?.let { File(it).toURI() }
			}

	private fun promiseContentUri(libraryId: LibraryId, serviceFile: ServiceFile, fileProperties: Map<String, String>): Promise<URI?> =
		mediaFileUriProvider
			.promiseUri(libraryId, serviceFile)
			.eventually { existingUri ->
				existingUri?.toURI()?.toPromise()
					?: QueuedPromise(CancellableMessageWriter { ct ->
						if (ct.isCancelled) return@CancellableMessageWriter null

						val newSongDetails = ContentValues().apply {
							put(MediaStore.Audio.Media.DISPLAY_NAME, fileProperties.baseFileNameAsMp3)
							put(MediaStore.Audio.Media.ARTIST, fileProperties.albumArtistOrArtist)
							put(MediaStore.Audio.Media.ALBUM, fileProperties[KnownFileProperties.Album])
							put(MediaStore.Audio.Media.IS_PENDING, 1)
						}

						if (ct.isCancelled) null
						else contentResolver
							.insert(MediaCollections.ExternalAudio, newSongDetails)
							?.toURI()
					}, ThreadPools.io)
			}
}
