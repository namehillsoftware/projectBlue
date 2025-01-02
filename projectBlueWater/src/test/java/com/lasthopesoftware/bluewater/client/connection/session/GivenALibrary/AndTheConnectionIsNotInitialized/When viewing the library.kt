package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsNotInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibrarySelectionNavigation
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When viewing the library` {
	companion object {
		private const val libraryId = 841
	}

	private val mut by lazy {
		LibrarySelectionNavigation(
			mockk {
				every { viewLibrary(LibraryId(libraryId)) } answers {
					viewedLibraryId = firstArg()
					Unit.toPromise()
				}

				every { viewApplicationSettings() } answers {
					applicationSettingsViewed = true
					Unit.toPromise()
				}
			},
			mockk {
				every { selectLibrary(any()) } answers { firstArg<LibraryId>().toPromise() }
			},
			mockk {
				every { initializeConnection(any()) } returns false.toPromise()
			}
		)
	}

	private var applicationSettingsViewed = false
	private var viewedLibraryId: LibraryId? = null

	@BeforeAll
	fun act() {
		mut.viewLibrary(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the viewed library is correct`() {
		assertThat(viewedLibraryId).isNull()
	}

	@Test
	fun `then the application settings are viewed`() {
		assertThat(applicationSettingsViewed).isTrue()
	}
}
