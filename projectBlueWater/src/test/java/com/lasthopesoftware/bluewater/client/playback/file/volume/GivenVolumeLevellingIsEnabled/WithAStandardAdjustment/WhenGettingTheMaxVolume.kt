package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsEnabled.WithAStandardAdjustment

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.offset
import org.junit.jupiter.api.Test

private const val libraryId = 940

class WhenGettingTheMaxVolume {

	private val returnedVolume by lazy {
		val connectionProvider = mockk<ProvideLibraryConnections> {
			every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(mockk<IConnectionProvider> {
				every { urlProvider } returns mockk {
					every { baseUrl } returns EmptyUrl.url
				}
			})
		}

		val repository = mockk<IFilePropertiesContainerRepository>()
		every {
			repository.getFilePropertiesContainer(UrlKeyHolder(EmptyUrl.url, ServiceFile(1)))
		} returns FilePropertiesContainer(0, mapOf(Pair(KnownFileProperties.VolumeLevelReplayGain, "-13.5")))

		val scopedRevisionProvider = mockk<CheckRevisions>()
		every { scopedRevisionProvider.promiseRevision(LibraryId(libraryId)) } returns 1.toPromise()

		val sessionFilePropertiesProvider = FilePropertiesProvider(connectionProvider, scopedRevisionProvider, repository)
		val cachedSessionFilePropertiesProvider = CachedFilePropertiesProvider(
			connectionProvider,
			repository,
			sessionFilePropertiesProvider
		)

		val volumeLevelSettings = mockk<IVolumeLevelSettings>()
		every { volumeLevelSettings.isVolumeLevellingEnabled } returns true.toPromise()

		val maxFileVolumeProvider = MaxFileVolumeProvider(volumeLevelSettings, cachedSessionFilePropertiesProvider)
		maxFileVolumeProvider.promiseMaxFileVolume(LibraryId(libraryId), ServiceFile(1)).toExpiringFuture().get()!!
	}

	@Test
	fun `then the returned volume is correct`() {
		assertThat(returnedVolume).isCloseTo(.2113489f, offset(.00001f))
	}
}
