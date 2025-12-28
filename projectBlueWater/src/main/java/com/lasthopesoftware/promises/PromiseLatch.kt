package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

class PromiseLatch : PromisingLatch, AutoCloseable {
	private val latch = AtomicReference(ResolvableBooleanPromise())

	@Volatile
	private var isClosed = false

	override fun open(): Gate {
		latch.get().resolve()
		return this
	}

	override fun reset(): Promise<Boolean> {
		return latch.updateAndGet {
			it.resolve()
			ResolvableBooleanPromise()
		}
	}

	override fun wait(): Promise<Boolean> = latch.get()

	override fun close() {
		isClosed = true
		latch.get().resolve()
	}

	private inner class ResolvableBooleanPromise : Promise<Boolean>() {
		init {
		    if (isClosed) resolve()
		}

		fun resolve() {
			resolve(isClosed)
		}
	}
}
