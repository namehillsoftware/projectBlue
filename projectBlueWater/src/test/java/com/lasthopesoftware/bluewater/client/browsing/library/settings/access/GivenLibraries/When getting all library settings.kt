package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenLibraries

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
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
import com.lasthopesoftware.resources.gson
import com.lasthopesoftware.resources.strings.EncryptedString
import com.lasthopesoftware.resources.strings.FakeStringGuard
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting all library settings` {


	private val mutt by lazy {
		LibrarySettingsAccess(
			FakeLibraryRepository(
				Library(
					id = 566,
					libraryName = "WBJ9Ay87S",
					isUsingExistingFiles = false,
					serverType = Library.ServerType.MediaCenter.name,
					nowPlayingId = 828,
					nowPlayingProgress = 870,
					savedTracksString = "xqZ3RYqDhyY",
					syncedFileLocation = SyncedFileLocation.INTERNAL,
					connectionSettings = StoredSettingsAndEncryptionCombiner.combineStoredSettingsAndEncryption(
						StoredMediaCenterConnectionSettings(
							accessCode = "UDHLsaeeIv",
							password = "paebSds",
							userName = "SzgouVHF",
							macAddress = "J7AJnGf1f",
							isLocalOnly = false,
							isWakeOnLanEnabled = false,
							sslCertificateFingerprint = "sf3gmyTQyx6",
							isSyncLocalConnectionsOnly = true,
						),
						StoredEncryptionSettings(
							password = "paebSds",
							initializationVector = "gk94iy8Lt5x",
							encryptionConfiguration = EncryptionConfiguration(
								padding = "90W21blyNg",
								algorithm = "17CQb5u6RPz",
								blockMode = "QWqupzRvp"
							)
						)
					)
				),
				Library(
					id = 986,
					libraryName = "XGBiOmj4GcU",
					isUsingExistingFiles = true,
					serverType = Library.ServerType.MediaCenter.name,
					nowPlayingId = 101,
					nowPlayingProgress = 371,
					savedTracksString = "VPiOKYtkSOV",
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = StoredSettingsAndEncryptionCombiner.combineStoredSettingsAndEncryption(
						StoredMediaCenterConnectionSettings(
							accessCode = "NQde3Dk",
							password = "b7fPnOx",
							userName = "Tuj4WES",
							macAddress = "hrSCxMJQMnT",
							isLocalOnly = true,
							isWakeOnLanEnabled = false,
							sslCertificateFingerprint = "D4hQF8cMazI",
							isSyncLocalConnectionsOnly = true,
						),
						StoredEncryptionSettings(
							password = "HQwMXrAY",
							initializationVector = "d1o1JMl9",
							encryptionConfiguration = EncryptionConfiguration(
								padding = "wIRTicIi",
								algorithm = "4h40LYxwSR",
								blockMode = "WCVqKyRiG"
							)
						)
					)
				),
				Library(
					id = 425,
					libraryName = "XB6g0oVn",
					isUsingExistingFiles = true,
					serverType = Library.ServerType.Subsonic.name,
					nowPlayingId = 263,
					nowPlayingProgress = 609,
					savedTracksString = "cI1lAeIi2",
					syncedFileLocation = SyncedFileLocation.INTERNAL,
					connectionSettings = gson.toJson(
						StoredSubsonicConnectionSettings(
							url = "CXF5gG5Sp1",
							password = "g8t5e2g",
							userName = "CxfB9NS",
							macAddress = "7fLLI84T",
							isWakeOnLanEnabled = false,
							sslCertificateFingerprint = "omlo1H0vILG",
						)
					)
				),
				Library(
					id = 293,
					libraryName = "01oouYaE",
					isUsingExistingFiles = false,
					serverType = "Accumsanconubia",
					nowPlayingId = 865,
					nowPlayingProgress = 823,
					savedTracksString = "VPiOKYtkSOV",
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = """
							{
							   "randomAccess": "YKYjYwo"
							}
						""".trimIndent()
				),
			),
			JsonEncoderDecoder,
			JsonEncoderDecoder,
			FakeStringGuard(
				mapOf(
					"BB2Y4g2g" to EncryptedString(
						protectedString = "paebSds",
						initializationVector = "gk94iy8Lt5x",
						padding = "90W21blyNg",
						algorithm = "17CQb5u6RPz",
						blockMode = "QWqupzRvp"
					),
					"a4WbQfA" to EncryptedString(
						protectedString = "HQwMXrAY",
						initializationVector = "d1o1JMl9",
						padding = "wIRTicIi",
						algorithm = "4h40LYxwSR",
						blockMode = "WCVqKyRiG"
					)
				)
			),
		)
	}

	private var librarySettings: Collection<LibrarySettings>? = null

	@BeforeAll
	fun act() {
		librarySettings = mutt.promiseAllLibrarySettings().toExpiringFuture().get()
	}

	@Test
	fun `then the settings are correct`() {
		assertThat(librarySettings).containsExactlyInAnyOrder(
			LibrarySettings(
				libraryId = LibraryId(566),
				libraryName = "WBJ9Ay87S",
				isUsingExistingFiles = false,
				syncedFileLocation = SyncedFileLocation.INTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "UDHLsaeeIv",
					password = "BB2Y4g2g",
					userName = "SzgouVHF",
					macAddress = "J7AJnGf1f",
					isLocalOnly = false,
					isWakeOnLanEnabled = false,
					sslCertificateFingerprint = "sf3gmyTQyx6",
					isSyncLocalConnectionsOnly = true,
				)
			),
			LibrarySettings(
				libraryId = LibraryId(986),
				libraryName = "XGBiOmj4GcU",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "NQde3Dk",
					password = "a4WbQfA",
					userName = "Tuj4WES",
					macAddress = "hrSCxMJQMnT",
					isLocalOnly = true,
					isWakeOnLanEnabled = false,
					sslCertificateFingerprint = "D4hQF8cMazI",
					isSyncLocalConnectionsOnly = true,
				)
			),
			LibrarySettings(
				libraryId = LibraryId(425),
				libraryName = "XB6g0oVn",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.INTERNAL,
				connectionSettings = StoredSubsonicConnectionSettings(
					url = "CXF5gG5Sp1",
					password = "g8t5e2g",
					userName = "CxfB9NS",
					macAddress = "7fLLI84T",
					isWakeOnLanEnabled = false,
					sslCertificateFingerprint = "omlo1H0vILG",
				),
			),
			LibrarySettings(
				libraryId = LibraryId(293),
				libraryName = "01oouYaE",
				isUsingExistingFiles = false,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = null
			),
		)
	}
}
