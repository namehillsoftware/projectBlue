package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsNotEnabled

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheMaxVolume {
	private val returnedVolume by lazy {
		val volumeLevelSettings = mockk<IVolumeLevelSettings>()
		every { volumeLevelSettings.isVolumeLevellingEnabled } returns Promise(false)
		val maxFileVolumeProvider = MaxFileVolumeProvider(volumeLevelSettings, mockk())

		maxFileVolumeProvider
			.promiseMaxFileVolume(LibraryId(244), ServiceFile(1))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheReturnedVolumeIsOne() {
		assertThat(returnedVolume).isEqualTo(1f)
	}
}
