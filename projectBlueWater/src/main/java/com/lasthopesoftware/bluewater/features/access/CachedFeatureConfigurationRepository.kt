package com.lasthopesoftware.bluewater.features.access

import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import java.util.concurrent.atomic.AtomicReference

class CachedFeatureConfigurationRepository(private val inner: HoldApplicationFeatureConfiguration) : HoldApplicationFeatureConfiguration {
	private val storedFeatureConfiguration = AtomicReference(RecurseOnRejectionLazyPromise())

	override fun promiseFeatureConfiguration(): Promise<ApplicationFeatureConfiguration> =
		storedFeatureConfiguration.get().`object`.originalPromise

	override fun promiseUpdatedFeatureConfiguration(applicationFeatureConfiguration: ApplicationFeatureConfiguration): Promise<ApplicationFeatureConfiguration> =
		inner
			.promiseUpdatedFeatureConfiguration(applicationFeatureConfiguration)
			.eventually {
				storedFeatureConfiguration.set(RecurseOnRejectionLazyPromise())
				promiseFeatureConfiguration()
			}

	private inner class RecurseOnRejectionLazyPromise
		: AbstractSynchronousLazy<ResolvedPromiseBox<ApplicationFeatureConfiguration, Promise<ApplicationFeatureConfiguration>>>(), ImmediateResponse<Throwable, Boolean> {
		override fun create(): ResolvedPromiseBox<ApplicationFeatureConfiguration, Promise<ApplicationFeatureConfiguration>> {
			val promise = inner.promiseFeatureConfiguration()
			promise.excuse(this)
			return ResolvedPromiseBox(promise)
		}

		// Reset the deck if the promise is rejected for any reason
		override fun respond(resolution: Throwable?): Boolean =
			storedFeatureConfiguration.compareAndSet(this, RecurseOnRejectionLazyPromise())
	}
}
