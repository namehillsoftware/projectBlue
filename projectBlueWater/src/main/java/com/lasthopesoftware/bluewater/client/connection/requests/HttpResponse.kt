package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.lasthopesoftware.resources.executors.ThreadPools
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
	get() = body.promiseReadAllBytes().cancelBackEventually { ThreadPools.compute.preparePromise { _ -> it.decodeToString() } }
