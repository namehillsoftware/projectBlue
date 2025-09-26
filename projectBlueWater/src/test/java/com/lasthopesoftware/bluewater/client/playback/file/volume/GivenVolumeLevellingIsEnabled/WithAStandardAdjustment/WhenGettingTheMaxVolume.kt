package com.lasthopesoftware.bluewater.client.playback.file.volume.GivenVolumeLevellingIsEnabled.WithAStandardAdjustment

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.offset
import org.junit.jupiter.api.Test

private const val libraryId = 940

class WhenGettingTheMaxVolume {

	private val returnedVolume by lazy {
		val maxFileVolumeProvider = MaxFileVolumeProvider(
			mockk {
				every { promiseIsVolumeLevellingEnabled() } returns true.toPromise()
				every { promiseIsPeakLevelNormalizeEnabled() } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile("1")) } returns mapOf(
					Pair(NormalizedFileProperties.VolumeLevelReplayGain, "-13.5"),
					Pair(NormalizedFileProperties.PeakGain, "20"),
				).toPromise()
			}
		)
		maxFileVolumeProvider.promiseMaxFileVolume(LibraryId(libraryId), ServiceFile("1")).toExpiringFuture().get()!!
	}

	@Test
	fun `then the returned volume is correct`() {
		assertThat(returnedVolume).isCloseTo(.2113489f, offset(.00001f))
	}
}
