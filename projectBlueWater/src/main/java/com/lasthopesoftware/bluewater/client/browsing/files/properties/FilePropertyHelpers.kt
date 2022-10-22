package com.lasthopesoftware.bluewater.client.browsing.files.properties

object FilePropertyHelpers {
	/*
	 * Get the duration of the serviceFile in milliseconds
	 */
	@JvmStatic
	fun parseDurationIntoMilliseconds(fileProperties: Map<String, String>): Long {
		val durationToParse = fileProperties[KnownFileProperties.Duration]
		return durationToParse?.toDoubleOrNull()?.let { it * 1000 }?.toLong() ?: -1
	}
}
