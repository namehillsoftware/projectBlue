package com.lasthopesoftware.bluewater.client.browsing.files.properties

/**
 * Created by david on 3/5/16.
 */
object FilePropertyHelpers {
    /*
	 * Get the duration of the serviceFile in milliseconds
	 */
	@JvmStatic
	fun parseDurationIntoMilliseconds(fileProperties: Map<String, String>): Int {
        val durationToParse = fileProperties[KnownFileProperties.Duration]
		return durationToParse?.toDoubleOrNull()?.let { it * 1000 }?.toInt() ?: -1
    }
}
