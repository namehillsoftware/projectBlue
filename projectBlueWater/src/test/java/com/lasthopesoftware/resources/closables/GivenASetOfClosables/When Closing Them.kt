package com.lasthopesoftware.resources.closables.GivenASetOfClosables

import com.lasthopesoftware.resources.closables.AutoCloseableManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Closing Them` {

	private val mut = AutoCloseableManager()

	private val closeables = listOf(Closeable(), Closeable(), Closeable())

	private val closedCloseables = mutableListOf<Closeable>()

	@BeforeAll
	fun act() {
		for (closeable in closeables) mut.manage(closeable)

		mut.close()
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
