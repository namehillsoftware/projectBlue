package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.parseDurationIntoMilliseconds
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<FilePropertiesPlayStatsUpdater>()

class FilePropertiesPlayStatsUpdater(
    private val filePropertiesProvider: ProvideLibraryFileProperties,
    private val filePropertiesStorage: UpdateFileProperties
) : UpdatePlaystats {
    override fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*> =
		filePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				try {
					val lastPlayedServer = fileProperties[KnownFileProperties.LAST_PLAYED]
					val duration = parseDurationIntoMilliseconds(fileProperties)
					val currentTime = System.currentTimeMillis()
					if (lastPlayedServer != null && currentTime - duration <= lastPlayedServer.toLong() * 1000) return@eventually Promise.empty<Collection<Unit>>()

					val numberPlaysString = fileProperties[KnownFileProperties.NUMBER_PLAYS]
					val numberPlays = numberPlaysString?.toIntOrNull() ?: 0
					val numberPlaysUpdate = filePropertiesStorage.promiseFileUpdate(
						libraryId,
						serviceFile,
						KnownFileProperties.NUMBER_PLAYS,
						numberPlays.inc().toString(),
						false
					)

					val newLastPlayed = (currentTime / 1000).toString()
					val lastPlayedUpdate = filePropertiesStorage.promiseFileUpdate(
						libraryId,
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
