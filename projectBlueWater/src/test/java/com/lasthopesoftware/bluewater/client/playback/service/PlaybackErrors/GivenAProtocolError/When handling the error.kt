package com.lasthopesoftware.bluewater.client.playback.service.PlaybackErrors.GivenAProtocolError

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackErrorHandler
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.ProtocolException

class `When handling the error` {

	private val mut by lazy {
		PlaybackErrorHandler(
			mockk(),
			mockk(),
			mockk {
				every { skipToNext() } answers {
					isSkipped = true
					Promise.empty()
				}
			},
			mockk {
				every { resume() } answers {
					isResumed = true
					Promise.empty()
				}
			},
			mockk(),
			mockk(),
			mockk(),
		)
	}

	private var isSkipped = false
	private var isResumed = false

	@BeforeAll
	fun act() {
		mut.onError(
			PreparationException(
				PositionedFile(
					128,
					ServiceFile("501")
				),
				PlaybackException(
					"oh no",
					HttpDataSource.HttpDataSourceException(
						ProtocolException("unexpected end of stream"),
						DataSpec(mockk()),
						PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
						HttpDataSource.HttpDataSourceException.TYPE_OPEN
					),
					PlaybackException.ERROR_CODE_UNSPECIFIED,
				)
			),
		)
	}

	@Test
	fun `then playback skips to the next file`() {
		assertThat(isSkipped).isTrue
	}

	@Test
	fun `then playback is resumed`() {
		assertThat(isResumed).isTrue
	}
}
