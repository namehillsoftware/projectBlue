package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseParsedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseSerializedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingRepository(
	private val libraryProvider: ISpecificLibraryProvider,
	private val libraryRepository: ILibraryStorage,
	private val holdNowPlayingState: HoldNowPlayingState
) : MaintainNowPlayingState {

	@Volatile
	private var trackedLibraryId: LibraryId? = null

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		trackedLibraryId
			?.let(holdNowPlayingState::get)?.toPromise()
			?: libraryProvider
				.promiseLibrary()
				.eventually { library ->
					library?.run {
						trackedLibraryId = libraryId
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

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> =
		trackedLibraryId
			?.let {
				holdNowPlayingState[it] = nowPlaying
				libraryProvider.promiseLibrary()
					.then { library ->
						library?.apply {
							setNowPlayingId(nowPlaying.playlistPosition)
							setNowPlayingProgress(nowPlaying.filePosition)
							setRepeating(nowPlaying.isRepeating)
							promiseSerializedFileStringList(nowPlaying.playlist)
								.then { serializedPlaylist ->
									setSavedTracksString(serializedPlaylist)
									libraryRepository.saveLibrary(this)
								}
						}
					}
				nowPlaying.toPromise()
			}
			?: promiseNowPlaying().eventually { updateNowPlaying(nowPlaying) }
}
