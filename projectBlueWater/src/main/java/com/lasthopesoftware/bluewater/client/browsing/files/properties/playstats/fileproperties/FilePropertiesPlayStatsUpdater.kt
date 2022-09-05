package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.parseDurationIntoMilliseconds
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<FilePropertiesPlayStatsUpdater>()

class FilePropertiesPlayStatsUpdater(
    private val filePropertiesProvider: ProvideScopedFileProperties,
    private val scopedFilePropertiesStorage: ScopedFilePropertiesStorage
) : UpdatePlaystats {
    override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> {
        return filePropertiesProvider.promiseFileProperties(serviceFile)
            .eventually { fileProperties ->
				try {
					val lastPlayedServer = fileProperties[KnownFileProperties.LAST_PLAYED]
					val duration = parseDurationIntoMilliseconds(fileProperties)
					val currentTime = System.currentTimeMillis()
					if (lastPlayedServer != null && currentTime - duration <= lastPlayedServer.toLong() * 1000) return@eventually Promise.empty<Collection<Unit>>()

					val numberPlaysString = fileProperties[KnownFileProperties.NUMBER_PLAYS]
					val numberPlays = numberPlaysString?.toIntOrNull() ?: 0
					val numberPlaysUpdate = scopedFilePropertiesStorage.promiseFileUpdate(
						serviceFile,
						KnownFileProperties.NUMBER_PLAYS,
						numberPlays.inc().toString(),
						false
					)

					val newLastPlayed = (currentTime / 1000).toString()
					val lastPlayedUpdate = scopedFilePropertiesStorage.promiseFileUpdate(
						serviceFile,
						KnownFileProperties.LAST_PLAYED,
						newLastPlayed,
						false
					)

					Promise.whenAll(numberPlaysUpdate, lastPlayedUpdate)
				} catch (ne: NumberFormatException) {
					logger.error(ne.toString(), ne)
					Promise.empty()
				}
			}
	}
}
