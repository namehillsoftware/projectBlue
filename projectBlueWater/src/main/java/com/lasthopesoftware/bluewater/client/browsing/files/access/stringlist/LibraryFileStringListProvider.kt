package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import okhttp3.Response

class LibraryFileStringListProvider(private val libraryConnections: ManageConnectionSessions) :
	ProvideFileStringListsForParameters,
	ImmediateResponse<Response?, String>
{

	override fun promiseFileStringList(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<String> {
		return libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connection ->
				connection
					?.promiseResponse(
						*FileListParameters.Helpers.processParams(
							option,
							*params
						)
					)
					.keepPromise()
			}
			.then(this)
	}

	override fun respond(response: Response?): String =
		response?.body?.use { body ->
			body.string()
		} ?: ""
}
