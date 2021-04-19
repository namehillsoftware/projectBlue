package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.LoadControl
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering.GetAudioRenderers
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.TimeoutException
import org.joda.time.Duration

class ExoPlayerPlaybackPreparer(
	private val context: Context,
	private val mediaSourceProvider: SpawnMediaSources,
	private val loadControl: LoadControl,
	private val renderersFactory: GetAudioRenderers,
	private val playbackHandler: Handler,
	private val playbackControlHandler: Handler,
	private val eventHandler: Handler,
	private val uriProvider: IFileUriProvider,
	private val configureExoPlayerPreparation: ConfigureExoPlayerPreparation) : PlayableFilePreparationSource {

	override fun promisePreparedPlaybackFile(serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile> =
		uriProvider.promiseFileUri(serviceFile)
			.eventually { uri ->
				val preparationTimeout = configureExoPlayerPreparation.preparationTimeout
				val promisedDelay = PromiseDelay.delay<PreparedPlayableFile>(preparationTimeout)
				val promisedTimeout = promisedDelay.then<PreparedPlayableFile> { throw TimeoutException("Timed out after $preparationTimeout") }
				val preparedExoPlayerPromise = PreparedExoPlayerPromise(
					context,
					mediaSourceProvider,
					loadControl,
					renderersFactory,
					playbackHandler,
					playbackControlHandler,
					eventHandler,
					uri,
					preparedAt)

				Promise.whenAny(preparedExoPlayerPromise, promisedTimeout).must {
					promisedDelay.cancel()
					preparedExoPlayerPromise.cancel()
				}
			}
}
