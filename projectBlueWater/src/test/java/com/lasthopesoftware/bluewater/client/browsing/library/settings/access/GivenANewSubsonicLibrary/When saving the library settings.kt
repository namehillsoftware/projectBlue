package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenANewSubsonicLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.LibrarySettingsAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.gson
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When saving the library settings` {

	private val mutt by lazy {
		LibrarySettingsAccess(
			mockk {
				every { promiseLibrary(any()) } returns Promise.empty()
				every { saveLibrary(any()) } answers {
					val library = firstArg<Library>()
					library.id = 479
					storedLibraries.add(library)
					library.toPromise()
				}
			},
			JsonEncoderDecoder,
		)
	}

	private var storedLibraries = mutableListOf<Library>()
	private var librarySettings: LibrarySettings? = null

	@BeforeAll
	fun act() {
		librarySettings = mutt.promiseSavedLibrarySettings(
			LibrarySettings(
				libraryName = "1qYq4sZ9H",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredSubsonicConnectionSettings(
					url = "TqWmHYwkF",
					macAddress = "IlcTP4W",
					isWakeOnLanEnabled = true,
					userName = "j7sRVmbpEg",
					password = "nrI3ZQp5",
					sslCertificateFingerprint = "nzMyG2g4abv"
				)
			)
		).toExpiringFuture().get()
	}

	@Test
	fun `then the saved settings are correct`() {
		assertThat(storedLibraries.single()).isEqualTo(
			Library(
				id = 479,
				libraryName = "1qYq4sZ9H",
				isUsingExistingFiles = true,
				serverType = Library.ServerType.Subsonic.name,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = gson.toJson(
					StoredSubsonicConnectionSettings(
						url = "TqWmHYwkF",
						macAddress = "IlcTP4W",
						isWakeOnLanEnabled = true,
						userName = "j7sRVmbpEg",
						password = "nrI3ZQp5",
						sslCertificateFingerprint = "nzMyG2g4abv"
					)
				)
			)
		)
	}

	@Test
	fun `then the returned settings are the saved settings`() {
		assertThat(librarySettings).isEqualTo(
			LibrarySettings(
				libraryId = LibraryId(479),
				libraryName = "1qYq4sZ9H",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredSubsonicConnectionSettings(
					url = "TqWmHYwkF",
					macAddress = "IlcTP4W",
					isWakeOnLanEnabled = true,
					userName = "j7sRVmbpEg",
					password = "nrI3ZQp5",
					sslCertificateFingerprint = "nzMyG2g4abv"
				)
			)
		)
	}
}
