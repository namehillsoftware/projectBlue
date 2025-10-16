package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenASavedMediaPlayerPlaybackEngineType

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.lasthopesoftware.resources.strings.parseJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When parsing the playback engine type` {
	private var playbackEngineType: PlaybackEngineType? = null

	@BeforeAll
	fun act() {
		playbackEngineType = JsonEncoderDecoder.parseJson<ApplicationFeatureConfiguration>(
			"""{
			|  "playbackEngineType": "MediaPlayer"
			|}""".trimMargin()
		)!!.playbackEngineType
	}

	@Test
	fun `then the playback engine type is correct`() {
		assertThat(playbackEngineType).isNull()
	}
}
