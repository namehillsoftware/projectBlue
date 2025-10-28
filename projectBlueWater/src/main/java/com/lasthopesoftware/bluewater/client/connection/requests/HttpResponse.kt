package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.lasthopesoftware.resources.io.PromisingReadableStream

interface HttpResponse : PromisingCloseable {
	val code: Int
	val message: String
	val headers: Map<String, List<String>>
	val body: PromisingReadableStream
	val contentLength: Long
}

val HttpResponse.isSuccessful
	get() = code in 200..299

val HttpResponse.bodyString
	get() = body.promiseReadAllBytes().then { it.decodeToString() }
