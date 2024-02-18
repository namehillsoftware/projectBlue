package com.lasthopesoftware.resources.closables.GivenASetOfPromisingClosables

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.PromisingCloseableManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Closing Them` {

	private val mut = PromisingCloseableManager()

	private val closeables = listOf(Closeable(), Closeable(), Closeable())

	private val closedCloseables = mutableListOf<Closeable>()

	@BeforeAll
	fun act() {
		for (closeable in closeables) mut.manage(closeable)

		mut.promiseClose().toExpiringFuture().get()
	}

	@Test
	fun `then the closeables are closed in the reverse order they were added`() {
		assertThat(closedCloseables).containsExactly(*closeables.reversed().toTypedArray())
	}

	inner class Closeable : AutoCloseable {
		override fun close() {
			closedCloseables.add(this)
		}
	}
}
