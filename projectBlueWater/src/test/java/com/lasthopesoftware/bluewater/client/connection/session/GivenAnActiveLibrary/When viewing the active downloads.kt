package com.lasthopesoftware.bluewater.client.connection.session.GivenAnActiveLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibrarySelectionNavigation
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When viewing the active downloads` {
	companion object {
		private const val libraryId = 104
	}

	private val mut by lazy {
		LibrarySelectionNavigation(
			mockk {
				every { viewActiveDownloads(LibraryId(libraryId)) } answers {
					viewedLibraryDownloadsId = firstArg()
					Unit.toPromise()
				}
			},
			mockk {
				every { promiseSelectedLibraryId() } returns LibraryId(libraryId).toPromise()
				every { selectBrowserLibrary(any()) } answers { Library(id = firstArg<LibraryId>().id).toPromise() }
			},
			mockk {
				every { initializeConnection(any()) } returns false.toPromise()
				every { initializeConnection(LibraryId(libraryId)) } returns true.toPromise()
			}
		)
	}

	private var viewedLibraryDownloadsId: LibraryId? = null

	@BeforeAll
	fun act() {
		mut.viewActiveDownloads().toExpiringFuture().get()
	}

	@Test
	fun `then the viewed library is correct`() {
		assertThat(viewedLibraryDownloadsId).isEqualTo(LibraryId(libraryId))
	}
}
