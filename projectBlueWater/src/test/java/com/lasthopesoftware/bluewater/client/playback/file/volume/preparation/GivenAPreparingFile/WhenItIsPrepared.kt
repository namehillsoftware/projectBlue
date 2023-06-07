package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.GivenAPreparingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.FakeFilePreparer
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

class WhenItIsPrepared {

	private val emptyPlaybackHandler = EmptyPlaybackHandler(0)
	private val returnedFile by lazy {
		val fakeFilePreparer = FakeFilePreparer(emptyPlaybackHandler, emptyPlaybackHandler)
		val maxFileVolumePreparer = MaxFileVolumePreparer(
			fakeFilePreparer,
			mockk {
				every { promiseMaxFileVolume(LibraryId(388), ServiceFile(5)) } returns Promise(.89f)
			}
		)
		maxFileVolumePreparer.promisePreparedPlaybackFile(
			LibraryId(388),
			ServiceFile(5),
			Duration.ZERO
		).toExpiringFuture().get()
	}

	@Test
	fun `then the file is returned`() {
		assertThat(returnedFile!!.playbackHandler).isEqualTo(emptyPlaybackHandler)
	}

	@Test
	fun `then the volume is managed by the max file volume manager`() {
		assertThat(returnedFile!!.playableFileVolumeManager.volume.toExpiringFuture().get()).isEqualTo(.89f)
	}
}
