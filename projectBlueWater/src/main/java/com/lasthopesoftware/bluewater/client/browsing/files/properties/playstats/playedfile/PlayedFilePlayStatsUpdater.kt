package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<PlayedFilePlayStatsUpdater>()

class PlayedFilePlayStatsUpdater(private val libraryConnections: ProvideLibraryConnections) : UpdatePlaystats {
    override fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { c -> c?.promiseResponse("File/Played", "File=" + serviceFile.key, "FileType=Key").keepPromise() }
			.then { response ->
				response?.use {
					val responseCode = it.code
					logger.debug("api/v1/File/Played responded with a response code of {}", responseCode)
					if (responseCode < 200 || responseCode >= 300) throw HttpResponseException(responseCode)
				}
			}
}
