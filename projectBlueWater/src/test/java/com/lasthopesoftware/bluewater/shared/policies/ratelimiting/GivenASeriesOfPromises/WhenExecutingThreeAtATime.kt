package com.lasthopesoftware.bluewater.shared.policies.ratelimiting.GivenASeriesOfPromises

import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.RateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Executors

class WhenExecutingThreeAtATime {

	companion object {

		private val expectedResult = Any()
		private val firstPromise = DeferredPromise(expectedResult)
		private val secondPromise = Promise<Any> { }
		private val thirdPromise = Promise<Any> { }
		private val fourthPromise = Promise<Any> { }
		private val fifthPromise = Promise<Any> { }
		private val sixthPromise = Promise<Any> { }
		private val activePromises = ArrayList<Promise<Any>>()
		private lateinit var firstResult: Any

		@JvmStatic
		@BeforeClass
		fun before() {

			val rateLimiter = RateLimiter<Any>(Executors.newCachedThreadPool(), 3)

			fun enqueuePromise(promise: Promise<Any>) =
				rateLimiter.limit { promise.also(activePromises::add).must { activePromises.remove(promise) } }

			val futureFirstResult = enqueuePromise(firstPromise).toFuture()
			enqueuePromise(secondPromise)
			enqueuePromise(thirdPromise)
			enqueuePromise(fourthPromise)
			enqueuePromise(fifthPromise)
			enqueuePromise(sixthPromise)
			firstPromise.resolve()
			firstResult = futureFirstResult.get()!!
		}
	}

	@Test
	fun thenThereAreThreeActivePromises() {
		assertThat(activePromises).containsOnly(secondPromise, thirdPromise, fourthPromise)
	}

	@Test
	fun thenTheFirstResultIsCorrect() {
		assertThat(firstResult).isEqualTo(expectedResult)
	}
}
