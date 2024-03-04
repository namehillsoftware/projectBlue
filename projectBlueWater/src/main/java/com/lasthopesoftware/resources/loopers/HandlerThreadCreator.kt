package com.lasthopesoftware.resources.loopers

import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import java.lang.Thread.UncaughtExceptionHandler

object HandlerThreadCreator {
	fun promiseNewHandlerThread(looperThreadName: String?, threadPriority: Int): Promise<CloseableHandlerThread> =
		object: Promise<CloseableHandlerThread>() {

			private val thread = object : CloseableHandlerThread(looperThreadName, threadPriority), CancellationResponse {
				override fun onLooperPrepared() {
					try {
						resolve(this)
					} catch (t: Throwable) {
						reject(t)
					}
				}

				override fun cancellationRequested() {
					if (state == State.NEW) {
						interrupt()
					}
				}
			}

			init {
				awaitCancellation(thread)

				with(thread) {
					val currentUncaughtExceptionHandler = uncaughtExceptionHandler
					uncaughtExceptionHandler = UncaughtExceptionHandler { t, e ->
						if (e is InterruptedException) {
							reject(e)
						}

						currentUncaughtExceptionHandler?.uncaughtException(t, e)
					}
					start()
				}
			}
		}
}
