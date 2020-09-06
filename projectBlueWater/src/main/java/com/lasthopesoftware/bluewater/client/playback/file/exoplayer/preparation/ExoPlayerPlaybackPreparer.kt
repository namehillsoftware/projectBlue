package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.GetAudioRenderers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.namehillsoftware.handoff.promises.Promise

class ExoPlayerPlaybackPreparer(
	private val context: Context,
	private val mediaSourceProvider: SpawnMediaSources,
	private val trackSelector: TrackSelector,
	private val loadControl: LoadControl,
	private val renderersFactory: GetAudioRenderers,
	private val handler: Handler,
	private val uriProvider: IFileUriProvider) : PlayableFilePreparationSource {

	override fun promisePreparedPlaybackFile(serviceFile: ServiceFile, preparedAt: Long): Promise<PreparedPlayableFile> =
		uriProvider.promiseFileUri(serviceFile)
			.eventually { uri ->
				PreparedExoPlayerPromise(
					context,
					mediaSourceProvider,
					trackSelector,
					loadControl,
					renderersFactory,
					handler,
					uri,
					preparedAt)
			}
}
