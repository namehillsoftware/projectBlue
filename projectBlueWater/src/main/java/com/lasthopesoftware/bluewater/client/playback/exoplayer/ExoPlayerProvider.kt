package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering.GetAudioRenderers
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ExoPlayerProvider(
	private val context: Context,
	private val renderersFactory: GetAudioRenderers,
	private val loadControl: LoadControl,
	private val playbackHandler: Handler,
) :
	ProvideExoPlayers,
	RenderersFactory,
	ImmediateResponse<Array<Renderer>, PromisingExoPlayer>
{
	private val promisedRenderers by lazy {

	}

	private lateinit var audioRenderers: Array<Renderer>

	override fun promiseExoPlayer(): Promise<PromisingExoPlayer> =
		renderersFactory
			.newRenderers()
			.then(this)

	override fun respond(renderers: Array<Renderer>): PromisingExoPlayer {
		audioRenderers = renderers

		val exoPlayerBuilder = ExoPlayer.Builder(context, this)
			.setLoadControl(loadControl)
			.setLooper(playbackHandler.looper)

		return HandlerDispatchingExoPlayer(exoPlayerBuilder.build(), playbackHandler)
	}

	override fun createRenderers(
		eventHandler: Handler,
		videoRendererEventListener: VideoRendererEventListener,
		audioRendererEventListener: AudioRendererEventListener,
		textRendererOutput: TextOutput,
		metadataRendererOutput: MetadataOutput
	): Array<Renderer> = audioRenderers
}
