package com.lasthopesoftware.bluewater.client.browsing.library.access.state.GivenLibraries

import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading the libraries` {
	private val mutt by lazy {
		LibraryListViewModel(
			mockk {
				every { promiseAllLibraries() } returns Promise(
					listOf(
						Library(id = 504),
						Library(id = 395),
						Library(id = 304),
						Library(id = 661),
					)
				)
			},
			mockk {
				every { promiseLibraryName(LibraryId(504)) } returns "RJvwnHp8".toPromise()
				every { promiseLibraryName(LibraryId(395)) } returns "sJF83GATo".toPromise()
				every { promiseLibraryName(LibraryId(304)) } returns "dNiotMiP".toPromise()
				every { promiseLibraryName(LibraryId(661)) } returns "Adutlms5vK7".toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		mutt.loadLibraries().toExpiringFuture().get()
	}

	@Test
	fun `then the libraries are returned and sorted correctly`() {
		assertThat(mutt.libraries.value).isEqualTo(
			listOf(
				Pair(LibraryId(304), "dNiotMiP"),
				Pair(LibraryId(395), "sJF83GATo"),
				Pair(LibraryId(504), "RJvwnHp8"),
				Pair(LibraryId(661), "Adutlms5vK7"),
			)
		)
	}
}
