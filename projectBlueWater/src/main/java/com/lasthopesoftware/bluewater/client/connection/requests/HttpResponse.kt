package com.lasthopesoftware.bluewater.client.connection.requests

import java.io.InputStream

interface HttpResponse : AutoCloseable {
	val code: Int
	val message: String
	val body: InputStream
}

val HttpResponse.bodyString
	get() = body.readBytes().decodeToString()
