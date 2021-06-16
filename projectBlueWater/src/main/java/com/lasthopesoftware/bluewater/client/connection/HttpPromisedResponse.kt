package com.lasthopesoftware.bluewater.client.connection

import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class HttpPromisedResponse(private val call: Call) : Promise<Response>(), Callback, Runnable {

	init {
		respondToCancellation(this)
		call.enqueue(this)
	}

	override fun onFailure(call: Call, e: IOException) {
		reject(e)
	}

	override fun onResponse(call: Call, response: Response) {
		resolve(response)
	}

	override fun run() {
		call.cancel()
	}
}
