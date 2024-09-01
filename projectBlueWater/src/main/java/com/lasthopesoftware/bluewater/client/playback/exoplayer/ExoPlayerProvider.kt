package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory

@OptIn(UnstableApi::class) class ExoPlayerProvider
	(
	private val context: Context,
	private val renderersFactory: RenderersFactory,
	private val loadControl: LoadControl,
	private val playbackHandler: Handler,
) :
	ProvideExoPlayers
{
	@OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getExoPlayer(): PromisingExoPlayer {
		val exoPlayerBuilder = ExoPlayer.Builder(context, renderersFactory)
			.setLoadControl(loadControl)
			.setLooper(playbackHandler.looper)

		return HandlerDispatchingExoPlayer(exoPlayerBuilder.build(), playbackHandler)
	}
}
