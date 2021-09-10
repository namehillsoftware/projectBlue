package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsNotEnabled

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingTheMaxVolume {
	companion object {
		private val returnedVolume by lazy {
			val scopedCachedFilePropertiesProvider = ScopedCachedFilePropertiesProvider(
				mockk(),
				mockk(),
				mockk()
			)
			val volumeLevelSettings = mockk<IVolumeLevelSettings>()
			every { volumeLevelSettings.isVolumeLevellingEnabled } returns Promise(false)
			val maxFileVolumeProvider =
				MaxFileVolumeProvider(volumeLevelSettings, scopedCachedFilePropertiesProvider)

			maxFileVolumeProvider
				.promiseMaxFileVolume(ServiceFile(1))
				.toFuture()
				.get()
		}
	}

	@Test
	fun thenTheReturnedVolumeIsOne() {
		assertThat(returnedVolume).isEqualTo(1f)
	}
}
