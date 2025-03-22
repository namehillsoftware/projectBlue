package com.lasthopesoftware.resources

import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import java.io.InputStream

class PassThroughHttpResponse(
	override val code: Int,
	override val message: String,
	override val body: InputStream,
) : HttpResponse {
	override fun close() {}
}
