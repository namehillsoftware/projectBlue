package com.lasthopesoftware.bluewater.client.connection.requests

import java.io.InputStream

interface HttpResponse : AutoCloseable {
	val code: Int
	val message: String
	val headers: Map<String, List<String>>
	val body: InputStream
	val contentLength: Long
}

val HttpResponse.isSuccessful
	get() = code in 200..299

val HttpResponse.bodyString
	get() = body.readBytes().decodeToString()
