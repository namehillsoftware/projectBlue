package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenANewLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.LibrarySettingsAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.gson
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
					library.id = 727
					storedLibraries.add(library)
					library.toPromise()
				}
			},
		)
	}

	private var storedLibraries = mutableListOf<Library>()
	private var librarySettings: LibrarySettings? = null

	@BeforeAll
	fun act() {
		librarySettings = mutt.promiseSavedLibrarySettings(
			LibrarySettings(
				libraryName = "sIrrIYGSkq",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.INTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "Wge53jlWyAg",
					macAddress = "2MpgnCVZBj",
					isLocalOnly = true,
					isWakeOnLanEnabled = false,
					isSyncLocalConnectionsOnly = false,
					userName = "SDuWsUvNkA",
					password = "GLYY75dLTI",
					sslCertificateFingerprint = "OJS0bIk"
				)
			)
		).toExpiringFuture().get()
	}

	@Test
	fun `then the saved settings are correct`() {
		assertThat(storedLibraries.single()).isEqualTo(
			Library(
				id = 727,
				libraryName = "sIrrIYGSkq",
				isUsingExistingFiles = true,
				serverType = Library.ServerType.MediaCenter.name,
				syncedFileLocation = SyncedFileLocation.INTERNAL,
				connectionSettings = gson.toJson(
					StoredMediaCenterConnectionSettings(
						accessCode = "Wge53jlWyAg",
						macAddress = "2MpgnCVZBj",
						isLocalOnly = true,
						isWakeOnLanEnabled = false,
						isSyncLocalConnectionsOnly = false,
						userName = "SDuWsUvNkA",
						password = "GLYY75dLTI",
						sslCertificateFingerprint = "OJS0bIk"
					)
				)
			)
		)
	}

	@Test
	fun `then the returned settings are the saved settings`() {
		assertThat(librarySettings).isEqualTo(
			LibrarySettings(
				libraryId = LibraryId(727),
				libraryName = "sIrrIYGSkq",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.INTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "Wge53jlWyAg",
					macAddress = "2MpgnCVZBj",
					isLocalOnly = true,
					isWakeOnLanEnabled = false,
					isSyncLocalConnectionsOnly = false,
					userName = "SDuWsUvNkA",
					password = "GLYY75dLTI",
					sslCertificateFingerprint = "OJS0bIk"
				)
			)
		)
	}
}
