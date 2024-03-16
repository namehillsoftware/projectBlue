package com.lasthopesoftware.bluewater.shared.policies.ratelimiting.GivenASeriesOfPromises

import com.lasthopesoftware.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenExecutingOneAtATime {

	private val expectedResult = Any()
	private val firstPromise = Promise(Any())
	private val secondPromise = Promise(Any())
	private val thirdPromise = DeferredPromise(expectedResult)
	private val fourthPromise = Promise<Any> { }
	private val fifthPromise = Promise<Any> { }
	private val sixthPromise = Promise<Any> { }
	private val activePromises = ArrayList<Promise<Any>>()
	private lateinit var thirdResult: Any

	@BeforeAll
	fun before() {

		val rateLimiter = PromisingRateLimiter<Any>(1)

		fun enqueuePromise(promise: Promise<Any>) =
			rateLimiter.limit { promise.also(activePromises::add).must { _ -> activePromises.remove(promise) } }

		enqueuePromise(firstPromise)
		enqueuePromise(secondPromise)
		val futureThirdResult = enqueuePromise(thirdPromise).toExpiringFuture()
		enqueuePromise(fourthPromise)
		enqueuePromise(fifthPromise)
		enqueuePromise(sixthPromise)
		thirdPromise.resolve()
		thirdResult = futureThirdResult.get()!!
	}

	@Test
	fun `then there is one active promise`() {
		assertThat(activePromises).containsOnly(fourthPromise)
	}

	@Test
	fun `then the first result is correct`() {
		assertThat(thirdResult).isEqualTo(expectedResult)
	}
}
