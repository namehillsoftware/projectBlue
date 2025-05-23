package com.lasthopesoftware.policies.retries

import com.lasthopesoftware.promises.NoopResponse
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

open class RetryOnRejectionLazyPromise<T>(private val factory: () -> Promise<T>) : Lazy<Promise<T>>, AutoCloseable {
	private val resolvedPromiseBox = AtomicReference(RecurseOnRejectionLazyPromise())

	@Volatile
	private var isClosed = false

	override val value: Promise<T>
		get() = resolvedPromiseBox.get().`object`.originalPromise

	fun isInitializing(): Boolean = resolvedPromiseBox.get().isCreated

	override fun isInitialized(): Boolean = resolvedPromiseBox.get().run {
		isCreated && `object`?.resolvedPromise != null
	}

	override fun close() {
		isClosed = true
		if (isInitializing())
			value.cancel()
	}

	private inner class RecurseOnRejectionLazyPromise
		: AbstractSynchronousLazy<ResolvedPromiseBox<T, Promise<T>>>(), ImmediateResponse<Throwable, Boolean> {
		override fun create(): ResolvedPromiseBox<T, Promise<T>> {
			if (isClosed) return ResolvedPromiseBox(Promise(CancellationException("${RetryOnRejectionLazyPromise::class.simpleName} is cancelled.")))

			val promise = factory()
			promise.excuse(this)
			return ResolvedPromiseBox(promise)
		}

		// Reset the deck if the promise is rejected for any reason
		override fun respond(resolution: Throwable?): Boolean =
			resolvedPromiseBox.compareAndSet(this, RecurseOnRejectionLazyPromise())
	}
}

class CloseableRetryOnRejectionLazyPromise<T : AutoCloseable>(factory: () -> Promise<T>) : RetryOnRejectionLazyPromise<T>(factory), PromisingCloseable, ImmediateResponse<T, Unit> {
	override fun promiseClose(): Promise<Unit> {
		close()
		return if (isInitializing()) value.then(this, NoopResponse.noOpResponse())
		else Unit.toPromise()
	}

	override fun respond(resolution: T) {
		resolution.close()
	}
}
