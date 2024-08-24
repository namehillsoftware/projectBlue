package com.lasthopesoftware.bluewater.client.browsing.files.properties

import org.apache.commons.io.FilenameUtils

object FilePropertyHelpers {
	/*
	 * Get the duration of the serviceFile in milliseconds
	 */
	fun parseDurationIntoMilliseconds(fileProperties: Map<String, String>): Long {
		val durationToParse = fileProperties[KnownFileProperties.Duration]
		return durationToParse?.toDoubleOrNull()?.let { it * 1000 }?.toLong() ?: -1
	}

	val Map<String, String>.albumArtistOrArtist
		get() = this[KnownFileProperties.AlbumArtist] ?: this[KnownFileProperties.Artist]

	val Map<String, String>.baseFileNameAsMp3
		get() = this[KnownFileProperties.Filename]
			?.let { f ->
				val fileName = FilenameUtils.getName(f)
				val parts = fileName.split(".", limit = 2)
				val baseName = parts[0]
				val postExtParts = parts.elementAtOrNull(1)?.let {
					val index = it.indexOf(';')
					if (index < 0) ""
					else it.substring(index)
				}
				"$baseName.mp3$postExtParts"
			}
}
