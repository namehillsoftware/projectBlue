package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import okhttp3.Response

class FileStringListProvider(private val selectedConnection: ProvideSelectedConnection) : ImmediateResponse<Response, String> {
	fun promiseFileStringList(option: FileListParameters.Options, vararg params: String): Promise<String> =
		selectedConnection.promiseSessionConnection()
			.eventually { connection ->
				connection
					?.promiseResponse(*FileListParameters.Helpers.processParams(option, *params))
					?.then(this)
					?: "".toPromise()
			}

	override fun respond(response: Response): String =
		response.body?.use { body ->
			body.string()
		} ?: ""
}
