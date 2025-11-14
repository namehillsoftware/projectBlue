package com.lasthopesoftware.bluewater.client.browsing.library.settings.access.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.LibrarySettingsAccess
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

class `When getting the library settings` {

	private val libraryId = LibraryId(698)

	private val mutt by lazy {
		val fakeLibraryRepository = FakeLibraryRepository()
		LibrarySettingsAccess(
			FakeLibraryRepository(
				Library(
					id = libraryId.id,
					libraryName = "cdtX4lVz",
					isUsingExistingFiles = true,
					serverType = Library.ServerType.MediaCenter.name,
					nowPlayingId = 499,
					nowPlayingProgress = 401,
					savedTracksString = "MN2zlU8F1w6",
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = gson.toJson(
						StoredMediaCenterConnectionSettings(
							accessCode = "DLKicYx",
							password = "LKHPUXwrF",
							userName = "UMm5g8jEN",
							macAddress = "fh1Y3MWf",
							isLocalOnly = true,
							isWakeOnLanEnabled = true,
							sslCertificateFingerprint = "8ZuBp6yyjiT",
							isSyncLocalConnectionsOnly = true,
						)
					)
				)
			),
			JsonEncoderDecoder,
			mockk {
				val encryptedString = EncryptedString(
					initializationVector = "H5FU96uOv",
					protectedString = "jeWIZHPHA",
					padding = "9LYqUKT0g",
					blockMode = "CDaNFV9aIX2",
					algorithm = "07IQKIPN6",
				)

				every { promiseEncryption("LKHPUXwrF") } returns encryptedString.toPromise()

				every { promiseDecryption(encryptedString) } returns "7cEveBV".toPromise()
			}
		)
	}

	private var librarySettings: LibrarySettings? = null

	@BeforeAll
	fun act() {
		librarySettings = mutt.promiseLibrarySettings(libraryId).toExpiringFuture().get()
	}

	@Test
	fun `then the settings are correct`() {
		assertThat(librarySettings).isEqualTo(
			LibrarySettings(
				libraryId = libraryId,
				libraryName = "cdtX4lVz",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "DLKicYx",
					password = "7cEveBV",
					initializationVector = "H5FU96uOv",
					userName = "UMm5g8jEN",
					macAddress = "fh1Y3MWf",
					isLocalOnly = true,
					isWakeOnLanEnabled = true,
					sslCertificateFingerprint = "8ZuBp6yyjiT",
					isSyncLocalConnectionsOnly = true,
					encryptionConfiguration = EncryptionConfiguration(
						padding = "9LYqUKT0g",
						blockMode = "CDaNFV9aIX2",
						algorithm = "07IQKIPN6",
					)
				)
			)
		)
	}
}
