package com.lasthopesoftware.bluewater.client.playback.file.DurationFallback.GivenAPlayingFile.AndItsDurationIsLessThanZero

import com.lasthopesoftware.bluewater.client.playback.file.DurationFallbackPlayingFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting the duration` {
	private val mut by lazy {
		DurationFallbackPlayingFile(
			mockk {
				every { duration } returns Duration.standardMinutes(-2L).toPromise()
			},
			mockk {
				every { duration } returns Duration.standardSeconds(975).toPromise()
			}
		)
	}

	private var duration: Duration? = null

	@BeforeAll
	fun act() {
		duration = mut.duration.toExpiringFuture().get()
	}

	@Test
	fun `then the duration is read from the fallback source`() {
		assertThat(duration).isEqualTo(Duration.standardSeconds(975))
	}
}
