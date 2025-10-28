package com.lasthopesoftware.resources

import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.io.PromisingReadableStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import java.io.InputStream

class PassThroughHttpResponse(
	override val code: Int,
	override val message: String,
	body: InputStream,
	override val headers: Map<String, List<String>> = emptyMap(),
	override val contentLength: Long = 0L,
) : HttpResponse {
	override val body: PromisingReadableStream = PromisingReadableStreamWrapper(body)
	override fun promiseClose(): Promise<Unit> = Unit.toPromise()
}
