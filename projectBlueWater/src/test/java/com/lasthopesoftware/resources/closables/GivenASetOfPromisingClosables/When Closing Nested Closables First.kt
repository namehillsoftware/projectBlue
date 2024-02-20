package com.lasthopesoftware.resources.closables.GivenASetOfPromisingClosables

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.PromisingCloseableManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Closing Nested Closables First` {
	private val mut = PromisingCloseableManager()

	private val closeables = listOf(Closeable(), Closeable(), Closeable())

	private val nestedCloseables = listOf(Closeable(), Closeable())

	private val doublyNestedCloseables = listOf(Closeable(), Closeable())

	private val closedCloseables = mutableListOf<Closeable>()

	@BeforeAll
	fun act() {
		with (mut) {
			for (closeable in closeables) manage(closeable)

			with (createNestedManager()) {
				for (closeable in nestedCloseables)
					manage(closeable)

				with (createNestedManager()) {
					for (closeable in doublyNestedCloseables)
						manage(closeable)
				}

				promiseClose().toExpiringFuture().get()
			}

			promiseClose().toExpiringFuture().get()
		}
	}

	@Test
	fun `then the closeables are closed in the reverse order they were added`() {
		assertThat(closedCloseables).containsExactly(
			*doublyNestedCloseables.reversed().toTypedArray(),
			*nestedCloseables.reversed().toTypedArray(),
			*closeables.reversed().toTypedArray()
		)
	}

	inner class Closeable : AutoCloseable {
		override fun close() {
			closedCloseables.add(this)
		}
	}
}
