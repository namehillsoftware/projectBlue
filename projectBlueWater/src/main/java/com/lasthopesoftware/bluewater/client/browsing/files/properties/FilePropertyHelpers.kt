package com.lasthopesoftware.bluewater.client.browsing.files.properties

import org.apache.commons.io.FilenameUtils

object FilePropertyHelpers {

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
}
