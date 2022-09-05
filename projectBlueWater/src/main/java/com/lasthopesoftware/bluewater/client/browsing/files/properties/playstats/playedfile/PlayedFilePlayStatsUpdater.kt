package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<PlayedFilePlayStatsUpdater>()

class PlayedFilePlayStatsUpdater(private val connectionProvider: IConnectionProvider) : UpdatePlaystats {
    override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> =
		connectionProvider
			.promiseResponse("File/Played", "File=" + serviceFile.key, "FileType=Key")
			.then { response ->
				response.use {
					val responseCode = it.code
					logger.debug("api/v1/File/Played responded with a response code of $responseCode")
					if (responseCode < 200 || responseCode >= 300) throw HttpResponseException(responseCode)
				}
			}
}
