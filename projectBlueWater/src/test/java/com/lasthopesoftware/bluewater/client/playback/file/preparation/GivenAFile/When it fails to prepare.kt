package com.lasthopesoftware.bluewater.client.playback.file.preparation.GivenAFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FallbackPlaybackPreparer
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When it fails to prepare` {
	companion object {
		private const val libraryId = 974
		private const val serviceFile = "okxd4aal"
	}

	private val mut by lazy {
		FallbackPlaybackPreparer(
			mockk {
				every { promisePreparedPlaybackFile(any(), any(), any()) } returns Promise(
					Exception("Oh no!")
				)
			},
			mockk {
				every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile(serviceFile), Duration.millis(864)) } returns fallbackFile.toPromise()
			}
		)
	}

	private val fallbackFile = FakePreparedPlayableFile(FakeBufferingPlaybackHandler())
	private var preparedPlayableFile: PreparedPlayableFile? = null

	@BeforeAll
	fun act() {
		preparedPlayableFile = mut.promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile(serviceFile), Duration.millis(864)).toExpiringFuture().get()
	}

	@Test
	fun `then the prepared playable file is correct`() {
		assertThat(preparedPlayableFile).isEqualTo(fallbackFile)
	}
}
