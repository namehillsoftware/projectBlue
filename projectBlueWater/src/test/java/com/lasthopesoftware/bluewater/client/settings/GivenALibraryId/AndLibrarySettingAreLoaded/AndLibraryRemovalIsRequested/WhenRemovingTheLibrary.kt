package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded.AndLibraryRemovalIsRequested

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRemovingTheLibrary {

	private val libraryId = LibraryId(208)
    private val services by lazy {
        LibrarySettingsViewModel(
            mockk {
				every { promiseLibrarySettings(any()) } answers {
					val id = firstArg<LibraryId>()
					if (removedLibraries.any { it == id }) Promise.empty()
					else LibrarySettings(libraryId = id).toPromise()
				}
			},
			mockk(),
			mockk {
				every { removeLibrary(any()) } answers {
					removedLibraries.add(firstArg())
					Unit.toPromise()
				}
			},
			mockk(),
			mockk(),
			FakeStringResources(),
		)
    }

	private val removedLibraries = mutableListOf<LibraryId>()

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()
			requestLibraryRemoval()
			removeLibrary().toExpiringFuture().get()
		}
    }

	@Test
	fun `then the library is removed`() {
		assertThat(removedLibraries.single()).isEqualTo(libraryId)
	}
}
