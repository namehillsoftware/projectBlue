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
				FilenameUtils.getBaseName(f) + ".mp3"
			}
}
