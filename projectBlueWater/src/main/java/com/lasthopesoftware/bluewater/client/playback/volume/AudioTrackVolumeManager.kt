package com.lasthopesoftware.bluewater.client.playback.volume

import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class AudioTrackVolumeManager(private val exoPlayer: PromisingExoPlayer, private val audioRenderers: Array<MediaCodecAudioRenderer>) : IVolumeManagement, ManagePlayableFileVolume {
	private var backingVolume = 0f

	override fun setVolume(volume: Float): Promise<Float> =
		Promise.whenAll(audioRenderers.map { renderer ->
			exoPlayer
				.createMessage(renderer)
				.then { m -> m.setType(Renderer.MSG_SET_VOLUME).setPayload(volume).send() }
		}).then {
			backingVolume = volume
			volume
		}

	override val volume: Promise<Float>
		get() = backingVolume.toPromise()
}
