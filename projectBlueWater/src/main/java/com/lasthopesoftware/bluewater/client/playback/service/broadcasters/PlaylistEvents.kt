package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName

/**
 * Created by david on 2/15/17.
 */
object PlaylistEvents {
	private val magicPropertyBuilder by lazy { MagicPropertyBuilder(PlaylistEvents::class.java) }

	@JvmField
	val onPlaylistStop = magicPropertyBuilder.buildProperty("onPlaylistStop")

	@JvmField
	val onPlaylistPause = magicPropertyBuilder.buildProperty("onPlaylistPause")

	val onPlaylistInterrupted by lazy { magicPropertyBuilder.buildProperty("onPlaylistInterrupted") }

	@JvmField
	val onPlaylistTrackComplete = magicPropertyBuilder.buildProperty("onPlaylistTrackComplete")

	val onPlaylistTrackStart by lazy { magicPropertyBuilder.buildProperty("onPlaylistTrackStart") }

	object PlaybackFileParameters {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(PlaybackFileParameters::class.java) }

		@JvmField
		val fileKey = magicPropertyBuilder.buildProperty("fileKey")

		val fileLibraryId by lazy { magicPropertyBuilder.buildProperty("fileLibraryId") }

		val isPlaying by lazy { magicPropertyBuilder.buildProperty("isPlaying") }
	}

	object PlaylistParameters {
		val playlistPosition by lazy { buildMagicPropertyName<PlaylistParameters>("playlistPosition") }
	}
}
