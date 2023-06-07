package com.lasthopesoftware.bluewater.shared.policies.retries

import com.lasthopesoftware.bluewater.shared.promises.ResolvedPromiseBox
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import java.util.concurrent.atomic.AtomicReference

class RetryOnRejectionLazyPromise<T>(private val factory: () -> Promise<T>) : Lazy<Promise<T>> {
	private val resolvedPromiseBox = AtomicReference(RecurseOnRejectionLazyPromise())

	override val value: Promise<T>
		get() = resolvedPromiseBox.get().`object`.originalPromise

	fun isInitializing(): Boolean = resolvedPromiseBox.get().isCreated

	override fun isInitialized(): Boolean = resolvedPromiseBox.get().run {
		isCreated && `object`?.resolvedPromise != null
	}

	private inner class RecurseOnRejectionLazyPromise
		: AbstractSynchronousLazy<ResolvedPromiseBox<T, Promise<T>>>(), ImmediateResponse<Throwable, Boolean> {
		override fun create(): ResolvedPromiseBox<T, Promise<T>> {
			val promise = factory()
			promise.excuse(this)
			return ResolvedPromiseBox(promise)
		}

		// Reset the deck if the promise is rejected for any reason
		override fun respond(resolution: Throwable?): Boolean =
			resolvedPromiseBox.compareAndSet(this, RecurseOnRejectionLazyPromise())
	}
}
