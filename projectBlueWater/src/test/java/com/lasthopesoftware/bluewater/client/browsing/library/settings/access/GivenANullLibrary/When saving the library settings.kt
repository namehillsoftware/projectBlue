package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenANullLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
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
					library.id = 940
					storedLibraries.add(library)
					library.toPromise()
				}
			},
			JsonEncoderDecoder,
			mockk(),
		)
	}

	private var storedLibraries = mutableListOf<Library>()
	private var librarySettings: LibrarySettings? = null

	@BeforeAll
	fun act() {
		librarySettings = mutt.promiseSavedLibrarySettings(
			LibrarySettings(
				libraryId = LibraryId(901),
				libraryName = "3YzKTfvRGcq",
				isUsingExistingFiles = false,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "nol6ZCLE",
					macAddress = "vOfUFH8a",
					isLocalOnly = false,
					isWakeOnLanEnabled = true,
					isSyncLocalConnectionsOnly = false,
					userName = "wWs0tKOH",
					password = "GLYY75dLTI",
					sslCertificateFingerprint = "EDxQcvG"
				)
			)
		).toExpiringFuture().get()
	}

	@Test
	fun `then the saved settings are correct`() {
		assertThat(storedLibraries.single()).isEqualTo(
			Library(
				id = 940,
				libraryName = "3YzKTfvRGcq",
				isUsingExistingFiles = false,
				serverType = Library.ServerType.MediaCenter.name,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = gson.toJson(
					StoredMediaCenterConnectionSettings(
						accessCode = "nol6ZCLE",
						macAddress = "vOfUFH8a",
						isLocalOnly = false,
						isWakeOnLanEnabled = true,
						isSyncLocalConnectionsOnly = false,
						userName = "wWs0tKOH",
						password = "GLYY75dLTI",
						sslCertificateFingerprint = "EDxQcvG"
					)
				)
			)
		)
	}

	@Test
	fun `then the returned settings are the saved settings`() {
		assertThat(librarySettings).isEqualTo(
			LibrarySettings(
				libraryId = LibraryId(940),
				libraryName = "3YzKTfvRGcq",
				isUsingExistingFiles = false,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "nol6ZCLE",
					macAddress = "vOfUFH8a",
					isLocalOnly = false,
					isWakeOnLanEnabled = true,
					isSyncLocalConnectionsOnly = false,
					userName = "wWs0tKOH",
					password = "GLYY75dLTI",
					sslCertificateFingerprint = "EDxQcvG"
				)
			)
		)
	}
}
