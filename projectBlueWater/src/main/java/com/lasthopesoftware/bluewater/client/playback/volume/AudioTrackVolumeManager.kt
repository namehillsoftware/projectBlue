package com.lasthopesoftware.bluewater.client.playback.volume

import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.namehillsoftware.handoff.promises.Promise

class AudioTrackVolumeManager(private val exoPlayer: PromisingExoPlayer, private val audioRenderers: Array<MediaCodecAudioRenderer>) : IVolumeManagement, ManagePlayableFileVolume {
	private var volume = 0f

	override fun setVolume(volume: Float): Promise<Float> {
		this.volume = volume
		return Promise.whenAll(audioRenderers.map { renderer ->
			exoPlayer
				.createMessage(renderer)
				.then { m -> m.setType(Renderer.MSG_SET_VOLUME).setPayload(this.volume).send() }
		}).then { this.volume }
	}

	override fun getVolume(): Float = volume
}
