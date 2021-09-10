package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsEnabled.WithAVeryHighAdjustment

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
import org.assertj.core.api.Assertions.assertThat
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
            } returns FilePropertiesContainer(0, mapOf(Pair(KnownFileProperties.VolumeLevelReplayGain, "25")))

			val scopedRevisionProvider = mockk<CheckScopedRevisions>()
			every { scopedRevisionProvider.promiseRevision() } returns 1.toPromise()

			val scopedFilePropertiesProvider =
                ScopedFilePropertiesProvider(connectionProvider, scopedRevisionProvider, repository)
            val scopedCachedFilePropertiesProvider = ScopedCachedFilePropertiesProvider(
                connectionProvider,
                repository,
                scopedFilePropertiesProvider
            )
            val volumeLevelSettings = mockk<IVolumeLevelSettings>()
            every { volumeLevelSettings.isVolumeLevellingEnabled } returns true.toPromise()

            val maxFileVolumeProvider =
                MaxFileVolumeProvider(volumeLevelSettings, scopedCachedFilePropertiesProvider)
			returnedVolume = maxFileVolumeProvider
                .promiseMaxFileVolume(ServiceFile(1))
				.toFuture()
				.get()!!
        }
    }

	@Test
	fun thenTheReturnedVolumeIsOne() {
		assertThat(returnedVolume).isEqualTo(1f)
	}
}
