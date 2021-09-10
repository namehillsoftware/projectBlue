package com.lasthopesoftware.bluewater.client.playback.volume

import com.google.android.exoplayer2.Renderer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class AudioTrackVolumeManager(private val exoPlayer: PromisingExoPlayer, private val audioRenderers: Array<Renderer>) : IVolumeManagement, ManagePlayableFileVolume {
	@Volatile
	private var backingVolume = 0f.toPromise()

	override fun setVolume(volume: Float): Promise<Float> {
		backingVolume = Promise
			.whenAll(audioRenderers.map { renderer ->
				exoPlayer
					.createMessage(renderer)
					.then { m -> m.setType(Renderer.MSG_SET_VOLUME).setPayload(volume).send() }
			})
			.then { volume }

		return backingVolume
	}

	override val volume: Promise<Float>
		get() = backingVolume
}
