package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.albumArtistOrArtist
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.baseFileNameAsMp3
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.namehillsoftware.handoff.promises.Promise
import org.apache.commons.io.FilenameUtils
import java.util.regex.Pattern

class StoredFilePathsLookup(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val lookupSyncDirectory: LookupSyncDirectory
) : GetStoredFilePaths {

	companion object {
		private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

		private fun String.replaceReservedCharsAndPath(): String =
			reservedCharactersPattern.matcher(this).replaceAll("_")
	}

	override fun promiseStoredFilePath(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String?> =
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				promiseLocalFilePath(libraryId, fileProperties)
			}

	private fun promiseLocalFilePath(libraryId: LibraryId, fileProperties: Map<String, String>): Promise<String?> =
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
			}
}
