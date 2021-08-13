package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsEnabled.WithAStandardAdjustment

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenGettingTheMaxVolume {

	companion object {
		private var returnedVolume = 0f

		@BeforeClass
		@JvmStatic
		fun before() {
			val urlProvider = mockk<IUrlProvider>()
			every { urlProvider.baseUrl } returns EmptyUrl.url
			val connectionProvider = mockk<IConnectionProvider>()
			every { connectionProvider.urlProvider } returns urlProvider

			val repository = mockk<IFilePropertiesContainerRepository>()
			every {
				repository.getFilePropertiesContainer(UrlKeyHolder(EmptyUrl.url, ServiceFile(1)))
			} returns FilePropertiesContainer(0, mapOf(Pair(KnownFileProperties.VolumeLevelReplayGain, "-13.5")))

			val scopedRevisionProvider = mockk<CheckScopedRevisions>()
			every { scopedRevisionProvider.promiseRevision() } returns 1.toPromise()

			val sessionFilePropertiesProvider = ScopedFilePropertiesProvider(connectionProvider, scopedRevisionProvider, repository)
			val cachedSessionFilePropertiesProvider = ScopedCachedFilePropertiesProvider(
				connectionProvider,
				repository,
				sessionFilePropertiesProvider)

			val volumeLevelSettings = mockk<IVolumeLevelSettings>()
			every { volumeLevelSettings.isVolumeLevellingEnabled } returns true

			val maxFileVolumeProvider = MaxFileVolumeProvider(volumeLevelSettings, cachedSessionFilePropertiesProvider)
			returnedVolume = maxFileVolumeProvider.promiseMaxFileVolume(ServiceFile(1)).toFuture().get()!!
		}
	}

	@Test
	fun thenTheReturnedVolumeIsCorrect() {
		Assertions.assertThat(returnedVolume).isCloseTo(.2113489f, Assertions.offset(.00001f))
	}
}
