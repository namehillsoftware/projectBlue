package com.lasthopesoftware.bluewater.client.library.items.media.files.access

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.util.*

abstract class AbstractFileResponder : PromisedResponse<String, Collection<ServiceFile>>, ImmediateResponse<Collection<ServiceFile>, List<ServiceFile>> {
	override fun promiseResponse(stringList: String): Promise<Collection<ServiceFile>> {
		return FileStringListUtilities.promiseParsedFileStringList(stringList)
	}

	override fun respond(serviceFiles: Collection<ServiceFile>): List<ServiceFile> {
		return if (serviceFiles is List<*>) serviceFiles as List<ServiceFile> else ArrayList(serviceFiles)
	}
}
