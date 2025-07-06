package com.lasthopesoftware.bluewater.client.playback.service.PlaybackErrors.GivenAResourceIsNotAvailableInTime

import com.lasthopesoftware.bluewater.client.playback.errors.PlaybackResourceNotAvailableInTimeException
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackErrorHandler
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When handling the error` {

	private val mut by lazy {
		PlaybackErrorHandler(
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			mockk {
				every { resetPlaylistManager() } answers {
					isReset = true
				}
			},
			mockk(),
		)
	}

	private var isReset = false

	@BeforeAll
	fun act() {
		mut.onError(
			PlaybackResourceNotAvailableInTimeException(
				"tests",
				Duration.standardSeconds(501)
			),
		)
	}

	@Test
	fun `then playback is reset`() {
		assertThat(isReset).isTrue
	}
}
