package com.lasthopesoftware.bluewater.client.stored.library.items.files.external.GivenContent

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalContentRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URI

class WhenRemovingTheContent : AndroidContext() {
	companion object {

		private val sut by lazy {
			ExternalContentRepository(
				mockk {
					every { delete(any(), null, null) } returns 464
				},
				mockk(),
			)
		}

		private var isRemoved = false
	}

	override fun before() {
		isRemoved = sut.removeContent(URI("content://ZqUbxDqDR")).toExpiringFuture().get() == true
	}

	@Test
	fun `then isRemoved is true`() {
		assertThat(isRemoved).isTrue
	}
}
