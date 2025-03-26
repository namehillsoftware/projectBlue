package com.lasthopesoftware.bluewater.client.browsing.library.access.names.GivenALibrary.AndItHasAnEmptyName

import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryNameLookup
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Getting the Library Name` {

	companion object {
		private const val libraryId = 746
	}

	private val mut by lazy {
		LibraryNameLookup(
            mockk {
                every { promiseLibrarySettings(LibraryId(libraryId)) } returns LibrarySettings(
                    libraryName = "",
                    connectionSettings = StoredMediaCenterConnectionSettings(accessCode = "q7hfTIL")
                ).toPromise()
            }
        )
	}

	private var name: String? = null

	@BeforeAll
	fun act() {
		name = mut.promiseLibraryName(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the library name is correct`() {
		assertThat(name).isEqualTo("q7hfTIL")
	}
}
