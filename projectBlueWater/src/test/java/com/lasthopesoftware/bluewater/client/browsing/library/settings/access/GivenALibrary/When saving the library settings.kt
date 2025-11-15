package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredEncryptionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.LibrarySettingsAccess
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.StoredSettingsAndEncryptionCombiner
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.encryption.EncryptionConfiguration
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.gson
import com.lasthopesoftware.resources.strings.EncryptedString
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.every
import io.mockk.mockk
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
					nowPlayingId = 322,
					nowPlayingProgress = 234,
					savedTracksString = "DSJaIU9",
					syncedFileLocation = SyncedFileLocation.INTERNAL,
					connectionSettings = gson.toJson(
						StoredMediaCenterConnectionSettings(
							accessCode = "8rl200k",
							password = "kKLGulh",
							userName = "RVeSIwbaSx",
							macAddress = "jiSBPtnU5C4",
							isLocalOnly = false,
							isWakeOnLanEnabled = false,
							sslCertificateFingerprint = "yYmFpbN7QIl",
							isSyncLocalConnectionsOnly = true,
						)
					)
				).toPromise()

				every { saveLibrary(any()) } answers {
					val library = firstArg<Library>()
					storedLibraries.add(library)
					library.toPromise()
				}
			},
			JsonEncoderDecoder,
			JsonEncoderDecoder,
			mockk {
				every { promiseEncryption("kKLGulh") } returns EncryptedString(
					initializationVector = "",
					"TGII5zyt",
					algorithm = "",
					blockMode = "",
					padding = "",
				).toPromise()

				val encryptedString = EncryptedString(
					initializationVector = "UylnoVbpoU",
					"SFzoA3Aw",
					algorithm = "68TeZkE4Pn",
					blockMode = "JW3Q32gyv3",
					padding = "XOAPwaYozTn",
				)
				every { promiseEncryption("urDDxiN") } returns encryptedString.toPromise()

				every { promiseDecryption(encryptedString) } returns "V7MbHDd".toPromise()
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
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredSubsonicConnectionSettings(
					url = "94NI0r9XP6x",
					userName = "RsZ6mNFK",
					password = "urDDxiN",
					isWakeOnLanEnabled = true,
					sslCertificateFingerprint = "KLSYONYutn",
					macAddress = "aRm4Zqh9O",
				)
			)
		).toExpiringFuture().get()
	}

	@Test
	fun `then the saved settings are correct`() {
		val expectedJsonSettings = StoredSettingsAndEncryptionCombiner.combineStoredSettingsAndEncryption(
			StoredSubsonicConnectionSettings(
				url = "94NI0r9XP6x",
				userName = "RsZ6mNFK",
				password = "SFzoA3Aw",
				isWakeOnLanEnabled = true,
				sslCertificateFingerprint = "KLSYONYutn",
				macAddress = "aRm4Zqh9O",
			),
			StoredEncryptionSettings(
				initializationVector = "UylnoVbpoU",
				password = "SFzoA3Aw",
				encryptionConfiguration = EncryptionConfiguration(
					algorithm = "68TeZkE4Pn",
					blockMode = "JW3Q32gyv3",
					padding = "XOAPwaYozTn",
				),
			)
		)

		assertThat(storedLibraries.single()).isEqualTo(
			Library(
				id = libraryId.id,
				libraryName = "jPStogpyW4",
				isUsingExistingFiles = false,
				serverType = Library.ServerType.Subsonic.name,
				nowPlayingId = 322,
				nowPlayingProgress = 234,
				savedTracksString = "DSJaIU9",
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = expectedJsonSettings
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
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredSubsonicConnectionSettings(
					url = "94NI0r9XP6x",
					userName = "RsZ6mNFK",
					password = "V7MbHDd",
					isWakeOnLanEnabled = true,
					sslCertificateFingerprint = "KLSYONYutn",
					macAddress = "aRm4Zqh9O",
				)
			)
		)
	}
}
