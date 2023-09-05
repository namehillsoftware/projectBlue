package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseParsedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseSerializedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingRepository(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val libraryRepository: ILibraryStorage,
	private val holdNowPlayingState: HoldNowPlayingState
) : MaintainNowPlayingState {

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> =
		selectedLibraryId
			.promiseSelectedLibraryId()
			.eventually { it?.let(::promiseNowPlaying).keepPromise() }

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> =
		holdNowPlayingState[libraryId]?.toPromise()
			?: libraryProvider
				.promiseLibrary(libraryId)
				.eventually { library ->
					library?.run {
						holdNowPlayingState[libraryId]?.toPromise()
							?: savedTracksString
							?.takeIf { it.isNotEmpty() }
							?.let(::promiseParsedFileStringList)
							?.then { files ->
								val nowPlaying = NowPlaying(
									libraryId,
									if (files is List<*>) files as List<ServiceFile> else ArrayList(files),
									nowPlayingId,
									nowPlayingProgress,
									isRepeating
								)
								holdNowPlayingState[libraryId] = nowPlaying
								nowPlaying
							}
							?: NowPlaying(
								libraryId,
								emptyList(),
								nowPlayingId,
								nowPlayingProgress,
								isRepeating
							).also { holdNowPlayingState[libraryId] = it }.toPromise()
					}.keepPromise()
				}

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		val libraryId = nowPlaying.libraryId

		holdNowPlayingState[libraryId] = nowPlaying

		libraryProvider
			.promiseLibrary(libraryId)
			.then { library ->
				library?.apply {
					nowPlayingId = nowPlaying.playlistPosition
					nowPlayingProgress = nowPlaying.filePosition
					isRepeating = nowPlaying.isRepeating
					promiseSerializedFileStringList(nowPlaying.playlist)
						.then { serializedPlaylist ->
							savedTracksString = serializedPlaylist
							libraryRepository.saveLibrary(this)
						}
				}
			}
		return nowPlaying.toPromise()
	}
}
