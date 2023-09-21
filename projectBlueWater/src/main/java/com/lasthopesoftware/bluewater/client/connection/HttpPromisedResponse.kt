package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

private val logger by lazyLogger<HttpPromisedResponse>()

class HttpPromisedResponse(private val call: Call) : Promise<Response>(), Callback, Runnable {

	init {
		respondToCancellation(this)
		call.enqueue(this)
	}

	override fun onFailure(call: Call, e: IOException) = reject(e)

	override fun onResponse(call: Call, response: Response) {
		resolve(response)

		if (response.isSuccessful || !DebugFlag.isDebugCompilation || !logger.isDebugEnabled) return

		response.body.use {
			logger.debug("Response returned error code {} with response {}.", response.code, it?.string() ?: "null")
		}
	}

	override fun run() = call.cancel()
}
