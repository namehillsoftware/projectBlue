package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.namehillsoftware.handoff.promises.Promise
import org.apache.commons.io.FilenameUtils
import java.util.regex.Pattern

class StoredFilePathsLookup(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val lookupSyncDirectory: LookupSyncDirectory,
) : GetStoredFilePaths {

	companion object {
		private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

		private fun replaceReservedCharsAndPath(path: String): String =
			reservedCharactersPattern.matcher(path).replaceAll("_")
	}

	override fun promiseStoredFilePath(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String?> =
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				lookupSyncDirectory
					.promiseSyncDirectory(libraryId)
					.then { syncDir ->
						syncDir
							?.path
							?.let { fullPath ->
								val artist = fileProperties[KnownFileProperties.ALBUM_ARTIST]
									?: fileProperties[KnownFileProperties.ARTIST]
								artist
									?.let { a ->
										FilenameUtils.concat(
											fullPath,
											replaceReservedCharsAndPath(a.trim { c -> c <= ' ' })
										)
									}
									?: fullPath
							}
							?.let { fullPath ->
								fileProperties[KnownFileProperties.ALBUM]
									?.let { album ->
										FilenameUtils.concat(
											fullPath,
											replaceReservedCharsAndPath(album.trim { it <= ' ' })
										)
									}
									?: fullPath
							}
							?.let { fullPath ->
								fileProperties[KnownFileProperties.FILENAME]
									?.let { f ->
										var lastPathIndex = f.lastIndexOf('\\')
										if (lastPathIndex < 0) lastPathIndex = f.lastIndexOf('/')
										if (lastPathIndex < 0) f
										else f.substring(lastPathIndex + 1)
											.let { newFileName ->
												val extensionIndex = newFileName.lastIndexOf('.')
												if (extensionIndex > -1) newFileName.substring(0, extensionIndex + 1) + "mp3"
												else newFileName
											}
									}
									?.let { fileName ->
										FilenameUtils.concat(fullPath, fileName).trim { it <= ' ' }
									}
									?: fullPath
							}
					}
			}
}
