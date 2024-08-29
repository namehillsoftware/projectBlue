package com.lasthopesoftware.bluewater.client.browsing.files.properties

import org.apache.commons.io.FilenameUtils
import java.util.regex.Pattern

object FilePropertyHelpers {

	private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

	private fun String.replaceReservedCharsAndPath(): String =
		reservedCharactersPattern.matcher(this).replaceAll("_")

	data class FileNameParts(val directory: String, val baseFileName: String, val ext: String, val postExtension: String)

	/*
	 * Get the duration of the serviceFile in milliseconds
	 */
	fun parseDurationIntoMilliseconds(fileProperties: Map<String, String>): Long {
		val durationToParse = fileProperties[KnownFileProperties.Duration]
		return durationToParse?.toDoubleOrNull()?.let { it * 1000 }?.toLong() ?: -1
	}

	val Map<String, String>.albumArtistOrArtist
		get() = this[KnownFileProperties.AlbumArtist] ?: this[KnownFileProperties.Artist]

	val Map<String, String>.fileNameParts
		get() = this[KnownFileProperties.Filename]
			?.let { f ->
				val path = FilenameUtils.getPath(f)
				val fileName = FilenameUtils.getName(f)

				var baseName = fileName
				var ext = ""
				val extensionIndex = fileName.lastIndexOf('.')
				if (extensionIndex > -1) {
					baseName = fileName.substring(0, extensionIndex)
					ext = fileName.substring(extensionIndex + 1)
				}

				val postExtParts = ext.let {
					val index = it.indexOf(';')
					if (index < 0) ""
					else it.substring(index)
				}

				FileNameParts(path, baseName, ext, postExtParts)
			}

	val Map<String, String>.baseFileNameAsMp3
		get() = fileNameParts
			?.run {
				"$baseFileName.mp3$postExtension"
			}

	val Map<String, String>.localExternalRelativeFileDirectory
		get() = albumArtistOrArtist?.trim { c -> c <= ' ' }?.replaceReservedCharsAndPath()
			?.let { path ->
				this[KnownFileProperties.Album]
					?.let { album ->
						FilenameUtils.concat(
							path, album.trim { it <= ' ' }.replaceReservedCharsAndPath()
						)
					}
					?: path
			}
			?.let { path -> if (!path.endsWith("/")) "$path/" else path }

	val Map<String, String>.localExternalRelativeFilePathAsMp3
		get() = localExternalRelativeFileDirectory?.let { dir ->
			baseFileNameAsMp3?.let { mp3 ->
					FilenameUtils.concat(dir, mp3).trim { it <= ' ' }
				}
			}
}
