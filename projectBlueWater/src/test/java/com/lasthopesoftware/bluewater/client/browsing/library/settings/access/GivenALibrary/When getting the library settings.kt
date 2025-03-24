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

class `When getting the library settings` {

	private val libraryId = LibraryId(698)

	private val mutt by lazy {
		LibrarySettingsAccess(
			mockk {
				every { promiseLibrary(libraryId) } returns Library(
					id = libraryId.id,
					libraryName = "cdtX4lVz",
					isUsingExistingFiles = true,
					serverType = Library.ServerType.MediaCenter,
					nowPlayingId = 499,
					nowPlayingProgress = 401,
					savedTracksString = "MN2zlU8F1w6",
					connectionSettings = Json.encodeToString(
						StoredMediaCenterConnectionSettings(
							accessCode = "DLKicYx",
							password = "LKHPUXwrF",
							userName = "UMm5g8jEN",
							macAddress = "fh1Y3MWf",
							isLocalOnly = true,
							syncedFileLocation = SyncedFileLocation.EXTERNAL,
							isWakeOnLanEnabled = true,
							sslCertificateFingerprint = "8ZuBp6yyjiT",
							isSyncLocalConnectionsOnly = true,
						)
					)
				).toPromise()
			},
			mockk()
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
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "DLKicYx",
					password = "LKHPUXwrF",
					userName = "UMm5g8jEN",
					macAddress = "fh1Y3MWf",
					isLocalOnly = true,
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					isWakeOnLanEnabled = true,
					sslCertificateFingerprint = "8ZuBp6yyjiT",
					isSyncLocalConnectionsOnly = true,
				)
			)
		)
	}
}
