package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateCancellableResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

inline fun <T, P> Promise<T>.cancelBackEventually(crossinline response: (T) -> Promise<P>): Promise<P> =
	object : Promise.Proxy<P>(), PromisedResponse<T, P> {
		init {
			proxy(
				this@cancelBackEventually
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(resolution: T): Promise<P> = response(resolution)
	}

inline fun <T, P> Promise<T>.cancelBackThen(crossinline response: (T, CancellationSignal) -> P): Promise<P> =
	object : Promise.Proxy<P>(), ImmediateCancellableResponse<T, P> {
		init {
			proxy(
				this@cancelBackThen
					.also(::doCancel)
					.then(this)
			)
		}

		override fun respond(resolution: T, c: CancellationSignal): P = response(resolution, c)
	}

inline fun <T, P> Promise<T>.regardless(crossinline response: () -> Promise<P>): Promise<P> =
	eventually(
		{ response() },
		{ response() }
	)
