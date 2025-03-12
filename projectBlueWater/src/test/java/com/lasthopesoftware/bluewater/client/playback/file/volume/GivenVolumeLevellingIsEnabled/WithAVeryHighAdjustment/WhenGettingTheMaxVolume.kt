package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsEnabled.WithAVeryHighAdjustment

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val libraryId = 940

class WhenGettingTheMaxVolume {

	private val returnedVolume by lazy {
		val volumeLevelSettings = mockk<IVolumeLevelSettings>()
		every { volumeLevelSettings.isVolumeLevellingEnabled } returns true.toPromise()

		val maxFileVolumeProvider = MaxFileVolumeProvider(
			volumeLevelSettings,
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(1)) } returns mapOf(Pair(KnownFileProperties.VolumeLevelReplayGain, "25")).toPromise()
			}
		)
		maxFileVolumeProvider
			.promiseMaxFileVolume(LibraryId(libraryId), ServiceFile(1))
			.toExpiringFuture()
			.get()!!
	}

	@Test
	fun `then the returned volume is one`() {
		assertThat(returnedVolume).isEqualTo(1f)
	}
}
