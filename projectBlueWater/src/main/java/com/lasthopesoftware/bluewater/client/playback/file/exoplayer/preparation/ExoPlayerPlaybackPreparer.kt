package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.ProvideBufferingExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class ExoPlayerPlaybackPreparer(
	private val mediaSourceProvider: SpawnMediaSources,
	private val provideExoPlayers: ProvideExoPlayers,
	private val provideBufferingExoPlayers: ProvideBufferingExoPlayers,
	private val uriProvider: ProvideFileUrisForLibrary
) : PlayableFilePreparationSource {

	override fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> =
		uriProvider
			.promiseUri(libraryId, serviceFile)
			.eventually { uri ->
				uri?.let {
					PreparedExoPlayerPromise(
						mediaSourceProvider,
						provideBufferingExoPlayers,
						provideExoPlayers,
						libraryId,
						it,
						preparedAt
					)
				}.keepPromise()
			}
}
