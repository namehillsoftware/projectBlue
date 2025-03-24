package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.LibrarySettingsAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When saving the library settings` {

	private val libraryId = LibraryId(132)

	private val mutt by lazy {
		LibrarySettingsAccess(
			mockk {
				every { promiseLibrary(libraryId) } returns Library(
					id = libraryId.id,
					libraryName = "i0UnMlt",
					isUsingExistingFiles = true,
					serverType = Library.ServerType.MediaCenter,
					nowPlayingId = 322,
					nowPlayingProgress = 234,
					savedTracksString = "DSJaIU9",
					connectionSettings = Json.encodeToString(
						StoredMediaCenterConnectionSettings(
							accessCode = "8rl200k",
							password = "kKLGulh",
							userName = "RVeSIwbaSx",
							macAddress = "jiSBPtnU5C4",
							isLocalOnly = false,
							syncedFileLocation = SyncedFileLocation.INTERNAL,
							isWakeOnLanEnabled = false,
							sslCertificateFingerprint = "yYmFpbN7QIl",
							isSyncLocalConnectionsOnly = true,
						)
					)
				).toPromise()
			},
			mockk {
				every { saveLibrary(any()) } answers {
					val library = firstArg<Library>()
					storedLibraries.add(library)
					library.toPromise()
				}
			}
		)
	}

	private var storedLibraries = mutableListOf<Library>()
	private var librarySettings: LibrarySettings? = null

	@BeforeAll
	fun act() {
		librarySettings = mutt.promiseSavedLibrarySettings(
			LibrarySettings(
				libraryId = libraryId,
				libraryName = "jPStogpyW4",
				isUsingExistingFiles = false,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "94NI0r9XP6x",
					macAddress = "aRm4Zqh9O",
					isLocalOnly = true,
					isWakeOnLanEnabled = true,
					isSyncLocalConnectionsOnly = false,
					userName = "RsZ6mNFK",
					password = "RZ5c4MGRC",
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					sslCertificateFingerprint = "KLSYONYutn"
				)
			)
		).toExpiringFuture().get()
	}

	@Test
	fun `then the saved settings are correct`() {
		assertThat(storedLibraries.single()).isEqualTo(
			Library(
				id = libraryId.id,
				libraryName = "jPStogpyW4",
				isUsingExistingFiles = false,
				serverType = Library.ServerType.MediaCenter,
				nowPlayingId = 322,
				nowPlayingProgress = 234,
				savedTracksString = "DSJaIU9",
				connectionSettings = Json.encodeToString(
					StoredMediaCenterConnectionSettings(
						accessCode = "94NI0r9XP6x",
						macAddress = "aRm4Zqh9O",
						isLocalOnly = true,
						isWakeOnLanEnabled = true,
						isSyncLocalConnectionsOnly = false,
						userName = "RsZ6mNFK",
						password = "RZ5c4MGRC",
						syncedFileLocation = SyncedFileLocation.EXTERNAL,
						sslCertificateFingerprint = "KLSYONYutn"
					)
				)
			)
		)
	}

	@Test
	fun `then the returned settings are the saved settings`() {
		assertThat(librarySettings).isEqualTo(
			LibrarySettings(
				libraryId = libraryId,
				libraryName = "jPStogpyW4",
				isUsingExistingFiles = false,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "94NI0r9XP6x",
					macAddress = "aRm4Zqh9O",
					isLocalOnly = true,
					isWakeOnLanEnabled = true,
					isSyncLocalConnectionsOnly = false,
					userName = "RsZ6mNFK",
					password = "RZ5c4MGRC",
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					sslCertificateFingerprint = "KLSYONYutn"
				)
			)
		)
	}
}
