package com.lasthopesoftware.resources.loopers

import com.namehillsoftware.handoff.promises.Promise
import java.lang.Thread.UncaughtExceptionHandler

object HandlerThreadCreator {
	fun promiseNewHandlerThread(looperThreadName: String?, threadPriority: Int): Promise<CloseableHandlerThread> =
		object: Promise<CloseableHandlerThread>(), Runnable {

			private val thread = object : CloseableHandlerThread(looperThreadName, threadPriority) {
				override fun onLooperPrepared() {
					try {
						resolve(this)
					} catch (t: Throwable) {
						reject(t)
					}
				}
			}

			init {
				respondToCancellation(this)

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

			override fun run() {
				if (thread.state == Thread.State.NEW) {
					thread.interrupt()
				}
			}
		}
}
