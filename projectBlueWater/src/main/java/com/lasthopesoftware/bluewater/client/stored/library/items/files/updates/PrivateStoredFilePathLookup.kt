package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.namehillsoftware.handoff.promises.Promise
import org.apache.commons.io.FilenameUtils
import java.util.regex.Pattern

class PrivateStoredFilePathLookup(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val lookupSyncDirectory: LookupSyncDirectory,
) : ProduceStoredFilePaths {

	companion object {
		private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

		private fun replaceReservedCharsAndPath(path: String): String =
			reservedCharactersPattern.matcher(path).replaceAll("_")
	}

	override fun promiseStoredFilePath(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String?> {
		return libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				lookupSyncDirectory
					.promiseSyncDirectory(libraryId)
					.then { syncDir ->
						var fullPath = syncDir?.path ?: return@then null

						val artist = fileProperties[KnownFileProperties.ALBUM_ARTIST]
							?: fileProperties[KnownFileProperties.ARTIST]
						if (artist != null) fullPath = FilenameUtils.concat(
							fullPath,
							replaceReservedCharsAndPath(artist.trim { it <= ' ' })
						)

						val album = fileProperties[KnownFileProperties.ALBUM]
						if (album != null) fullPath = FilenameUtils.concat(
							fullPath,
							replaceReservedCharsAndPath(album.trim { it <= ' ' })
						)

						val fileName = fileProperties[KnownFileProperties.FILENAME]?.let { f ->
							var lastPathIndex = f.lastIndexOf('\\')
							if (lastPathIndex < 0) lastPathIndex = f.lastIndexOf('/')
							if (lastPathIndex < 0) f
							else {
								var newFileName = f.substring(lastPathIndex + 1)
								val extensionIndex = newFileName.lastIndexOf('.')
								if (extensionIndex > -1)
									newFileName = newFileName.substring(0, extensionIndex + 1) + "mp3"
								newFileName
							}
						}
						FilenameUtils.concat(fullPath, fileName).trim { it <= ' ' }
					}
			}
	}
}
