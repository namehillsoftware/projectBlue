package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

object FileResponses : PromisedResponse<String, Collection<ServiceFile>>, ImmediateResponse<Collection<ServiceFile>, List<ServiceFile>> {
	private val emptyListPromise by lazy { Promise<Collection<ServiceFile>>(emptyList()) }

	override fun promiseResponse(stringList: String?): Promise<Collection<ServiceFile>> {
		return stringList?.let(FileStringListUtilities::promiseParsedFileStringList) ?: emptyListPromise
	}

	override fun respond(serviceFiles: Collection<ServiceFile>): List<ServiceFile> {
		return if (serviceFiles is List<*>) serviceFiles as List<ServiceFile> else serviceFiles.toList()
	}
}
