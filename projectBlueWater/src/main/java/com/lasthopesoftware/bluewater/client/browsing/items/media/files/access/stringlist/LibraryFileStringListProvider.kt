package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import okhttp3.Response

class LibraryFileStringListProvider(private val libraryConnections: ConnectionSessionManager) : ImmediateResponse<Response, String> {

	fun promiseFileStringList(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<String> {
		return libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connection ->
				connection?.promiseResponse(
					*FileListParameters.Helpers.processParams(
						option,
						*params
					)
				)
			}
			.then(this)
	}

	override fun respond(response: Response?): String =
		response?.body?.use { body ->
			body.string()
		} ?: ""
}
