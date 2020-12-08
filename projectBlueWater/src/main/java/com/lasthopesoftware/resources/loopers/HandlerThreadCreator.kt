package com.lasthopesoftware.resources.loopers

import android.os.HandlerThread
import com.namehillsoftware.handoff.promises.Promise

object HandlerThreadCreator {
	@JvmStatic
	fun promiseNewHandlerThread(looperThreadName: String?, threadPriority: Int): Promise<HandlerThread> {
		return object: Promise<HandlerThread>() {
			init {
				object : HandlerThread(looperThreadName, threadPriority) {
					override fun onLooperPrepared() {
						try {
							resolve(this)
						} catch (t: Throwable) {
							reject(t)
						}
					}
				}.start()
			}
		}
	}
}
