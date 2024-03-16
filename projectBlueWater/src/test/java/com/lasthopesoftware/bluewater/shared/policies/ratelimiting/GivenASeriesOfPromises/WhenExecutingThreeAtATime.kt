package com.lasthopesoftware.bluewater.shared.policies.ratelimiting.GivenASeriesOfPromises

import com.lasthopesoftware.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenExecutingThreeAtATime {

	private val expectedResult = Any()
	private val firstPromise = DeferredPromise(expectedResult)
	private val secondPromise = Promise<Any> { }
	private val thirdPromise = Promise<Any> { }
	private val fourthPromise = Promise<Any> { }
	private val fifthPromise = Promise<Any> { }
	private val sixthPromise = Promise<Any> { }
	private val activePromises = ArrayList<Promise<Any>>()
	private lateinit var firstResult: Any

	@BeforeAll
	fun before() {

		val rateLimiter = PromisingRateLimiter<Any>(3)

		fun enqueuePromise(promise: Promise<Any>) =
			rateLimiter.limit { promise.also(activePromises::add).must { _ -> activePromises.remove(promise) } }

		val futureFirstResult = enqueuePromise(firstPromise).toExpiringFuture()
		enqueuePromise(secondPromise)
		enqueuePromise(thirdPromise)
		enqueuePromise(fourthPromise)
		enqueuePromise(fifthPromise)
		enqueuePromise(sixthPromise)
		firstPromise.resolve()
		firstResult = futureFirstResult.get()!!
	}

	@Test
	fun `then there are three active promises`() {
		assertThat(activePromises).containsOnly(secondPromise, thirdPromise, fourthPromise)
	}

	@Test
	fun `then the first result is correct`() {
		assertThat(firstResult).isEqualTo(expectedResult)
	}
}
