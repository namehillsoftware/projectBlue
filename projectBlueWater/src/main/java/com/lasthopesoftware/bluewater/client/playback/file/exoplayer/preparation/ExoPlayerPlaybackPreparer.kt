package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.os.Handler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class ExoPlayerPlaybackPreparer(
	private val mediaSourceProvider: SpawnMediaSources,
	private val provideExoPlayers: ProvideExoPlayers,
	private val eventHandler: Handler,
	private val uriProvider: IFileUriProvider
) : PlayableFilePreparationSource {

	override fun promisePreparedPlaybackFile(serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> =
		uriProvider
			.promiseFileUri(serviceFile)
			.eventually { uri ->
				uri?.let {
					PreparedExoPlayerPromise(
						mediaSourceProvider,
						eventHandler,
						provideExoPlayers,
						it,
						preparedAt
					)
				}.keepPromise()
			}
}
