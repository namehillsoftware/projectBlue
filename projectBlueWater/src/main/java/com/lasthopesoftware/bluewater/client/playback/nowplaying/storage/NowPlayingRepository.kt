package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseParsedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseSerializedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class NowPlayingRepository(
	private val libraryProvider: ISpecificLibraryProvider,
	private val libraryRepository: ILibraryStorage
) : MaintainNowPlayingState {

	companion object {
		private val nowPlayingCache = ConcurrentHashMap<Int, NowPlaying>()
	}

	@Volatile
	private var trackedLibraryId = -1

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		nowPlayingCache[trackedLibraryId]?.toPromise()
			?: libraryProvider.library
				.eventually { library ->
					library?.run {
						trackedLibraryId = id
						nowPlayingCache[id]?.toPromise()
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
								nowPlayingCache[id] = nowPlaying
								nowPlaying
							}
							?: NowPlaying(
								libraryId,
								emptyList(),
								nowPlayingId,
								nowPlayingProgress,
								isRepeating
							).also { nowPlayingCache[id] = it }.toPromise()
					}.keepPromise()
				}

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		if (trackedLibraryId < 0) return promiseNowPlaying().eventually { updateNowPlaying(nowPlaying) }
		nowPlayingCache[trackedLibraryId] = nowPlaying
		libraryProvider.library
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
		return nowPlaying.toPromise()
	}
}
