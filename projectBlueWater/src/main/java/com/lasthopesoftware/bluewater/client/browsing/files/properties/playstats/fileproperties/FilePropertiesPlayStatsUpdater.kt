package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.durationInMs
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<FilePropertiesPlayStatsUpdater>()

class FilePropertiesPlayStatsUpdater(
    private val filePropertiesProvider: ProvideFreshLibraryFileProperties,
    private val filePropertiesStorage: UpdateFileProperties
) : UpdatePlayStatsWithFileProperties {
    override fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*> =
		filePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				try {
					val lastPlayedServer = fileProperties[KnownFileProperties.LastPlayed]
					val duration = fileProperties.durationInMs ?: 0
					val currentTime = System.currentTimeMillis()
					if (lastPlayedServer != null && currentTime - duration <= lastPlayedServer.toLong() * 1000) return@eventually Promise.empty<Collection<Unit>>()

					val numberPlaysString = fileProperties[KnownFileProperties.NumberPlays]
					val numberPlays = numberPlaysString?.toIntOrNull() ?: 0
					val numberPlaysUpdate = filePropertiesStorage.promiseFileUpdate(
						libraryId,
						serviceFile,
						KnownFileProperties.NumberPlays,
						numberPlays.inc().toString(),
						false
					)

					val newLastPlayed = (currentTime / 1000).toString()
					val lastPlayedUpdate = filePropertiesStorage.promiseFileUpdate(
						libraryId,
						serviceFile,
						KnownFileProperties.LastPlayed,
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
