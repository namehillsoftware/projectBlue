package com.lasthopesoftware.bluewater.shared.policies.ratelimiting.GivenASeriesOfPromises

import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenExecutingOneAtATime {

	companion object {

		private val expectedResult = Any()
		private val firstPromise = Promise(Any())
		private val secondPromise = Promise(Any())
		private val thirdPromise = DeferredPromise(expectedResult)
		private val fourthPromise = Promise<Any> { }
		private val fifthPromise = Promise<Any> { }
		private val sixthPromise = Promise<Any> { }
		private val activePromises = ArrayList<Promise<Any>>()
		private lateinit var thirdResult: Any

		@JvmStatic
		@BeforeClass
		fun before() {

			val rateLimiter = PromisingRateLimiter<Any>(1)

			fun enqueuePromise(promise: Promise<Any>) =
				rateLimiter.limit { promise.also(activePromises::add).must { activePromises.remove(promise) } }

			enqueuePromise(firstPromise)
			enqueuePromise(secondPromise)
			val futureThirdResult = enqueuePromise(thirdPromise).toExpiringFuture()
			enqueuePromise(fourthPromise)
			enqueuePromise(fifthPromise)
			enqueuePromise(sixthPromise)
			thirdPromise.resolve()
			thirdResult = futureThirdResult.get()!!
		}
	}

	@Test
	fun thenThereIsOneActivePromise() {
		assertThat(activePromises).containsOnly(fourthPromise)
	}

	@Test
	fun thenTheFirstResultIsCorrect() {
		assertThat(thirdResult).isEqualTo(expectedResult)
	}
}
