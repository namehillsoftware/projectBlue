package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.baseFileNameAsMp3
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.localExternalRelativeFileDirectory
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.localExternalRelativeFilePathAsMp3
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalMusicContent
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.HaveExternalContent
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.uri.toURI
import com.namehillsoftware.handoff.promises.Promise
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.URI

class StoredFileUrisLookup(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val libraryProvider: ILibraryProvider,
	private val lookupSyncDirectory: LookupSyncDirectory,
	private val mediaFileUriProvider: MediaFileUriProvider,
	private val externalContent: HaveExternalContent,
) : GetStoredFileUris {

	override fun promiseStoredFileUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<URI?> =
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				libraryProvider
					.promiseLibrary(libraryId)
					.eventually { l ->
						when (l?.syncedFileLocation) {
							Library.SyncedFileLocation.INTERNAL -> promiseLocalFileUri(libraryId, fileProperties)
							Library.SyncedFileLocation.EXTERNAL -> promiseExternalUri(libraryId, serviceFile, fileProperties)
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
							.localExternalRelativeFilePathAsMp3
							?.let { mp3 ->
									FilenameUtils.concat(fullPath, mp3)
							}
							?: fullPath
					}
					?.let { File(it).toURI() }
			}

	private fun promiseExternalUri(libraryId: LibraryId, serviceFile: ServiceFile, fileProperties: Map<String, String>): Promise<URI?> =
		mediaFileUriProvider
			.promiseUri(libraryId, serviceFile)
			.eventually { existingUri ->
				existingUri?.toURI()?.toPromise() ?: externalContent.promiseNewContentUri(
					ExternalMusicContent(
						displayName = fileProperties.baseFileNameAsMp3,
						relativePath = fileProperties.localExternalRelativeFileDirectory
					)
				)
			}
}
