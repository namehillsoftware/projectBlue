package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.util.*

abstract class AbstractFileResponder : PromisedResponse<String, Collection<ServiceFile>>, ImmediateResponse<Collection<ServiceFile>, List<ServiceFile>> {
	final override fun promiseResponse(stringList: String?): Promise<Collection<ServiceFile>> {
		return stringList?.let(FileStringListUtilities::promiseParsedFileStringList) ?: Promise(emptyList())
	}

	final override fun respond(serviceFiles: Collection<ServiceFile>): List<ServiceFile> {
		return if (serviceFiles is List<*>) serviceFiles as List<ServiceFile> else ArrayList(serviceFiles)
	}
}
