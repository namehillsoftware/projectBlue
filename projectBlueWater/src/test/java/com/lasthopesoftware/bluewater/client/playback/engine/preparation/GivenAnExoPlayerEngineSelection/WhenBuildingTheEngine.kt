package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAnExoPlayerEngineSelection

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenBuildingTheEngine {
	private val engine by lazy {
		val lookupSelectedPlaybackEngineType = mockk<LookupSelectedPlaybackEngineType>().apply {
			every { promiseSelectedPlaybackEngineType() } returns PlaybackEngineType.ExoPlayer.toPromise()
		}

		val playbackEngineBuilder = PreparedPlaybackQueueFeederBuilder(
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			mockk(),
		)
		playbackEngineBuilder.build(Library())
	}

	@Test
	fun `then an exo player engine is built`() {
		assertThat(engine).isInstanceOf(
			ExoPlayerPlayableFilePreparationSourceProvider::class.java
		)
	}
}
