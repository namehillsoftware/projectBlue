package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import org.joda.time.Duration

object DefaultExoPlayerPreparationConfiguration : ConfigureExoPlayerPreparation {
	private val staticTimeout = Duration.standardSeconds(45)

	override val preparationTimeout: Duration
		get() = staticTimeout
}
