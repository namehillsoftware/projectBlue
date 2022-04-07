package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.RenderersFactory

class ExoPlayerProvider(
	private val context: Context,
	private val renderersFactory: RenderersFactory,
	private val loadControl: LoadControl,
	private val playbackHandler: Handler,
) :
	ProvideExoPlayers
{
	override fun getExoPlayer(): PromisingExoPlayer {
		val exoPlayerBuilder = ExoPlayer.Builder(context, renderersFactory)
			.setLoadControl(loadControl)
			.setLooper(playbackHandler.looper)

		return HandlerDispatchingExoPlayer(exoPlayerBuilder.build(), playbackHandler)
	}
}
