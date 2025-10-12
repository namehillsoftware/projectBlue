package com.lasthopesoftware.resources

import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import java.io.InputStream

class PassThroughHttpResponse(
	override val code: Int,
	override val message: String,
	override val body: InputStream,
	override val headers: Map<String, List<String>> = emptyMap(),
	override val contentLength: Long = 0L,
) : HttpResponse {
	override fun close() {}
}
