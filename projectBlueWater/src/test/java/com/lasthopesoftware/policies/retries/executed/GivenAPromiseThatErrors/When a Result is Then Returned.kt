package com.lasthopesoftware.policies.retries.executed.GivenAPromiseThatErrors

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.policies.retries.ExecutedPromiseRetryHandler
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When a Result is Then Returned` {

	private var result: String? = null

	@BeforeAll
	fun act() {
		result = ExecutedPromiseRetryHandler.retryOnException { e ->
			if (e == null) Promise(Exception("hello")) else "bean".toPromise()
		}.toExpiringFuture().get()
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isEqualTo("bean")
	}
}
