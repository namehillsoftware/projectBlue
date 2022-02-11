package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities.promiseParsedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities.promiseSerializedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class NowPlayingRepository(
    private val libraryProvider: ISpecificLibraryProvider,
    private val libraryRepository: ILibraryStorage
) : INowPlayingRepository {

	companion object {
		private val nowPlayingCache = ConcurrentHashMap<Int, NowPlaying>()
	}

    @Volatile
    private var libraryId = -1

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
    	nowPlayingCache[libraryId]?.toPromise()
			?: libraryProvider.library
            .eventually { library ->
				library?.let {
					libraryId = it.id
					val savedTracksString = library.savedTracksString
					if (savedTracksString == null || savedTracksString.isEmpty()) {
						val nowPlaying = NowPlaying(
							library.nowPlayingId,
							library.nowPlayingProgress,
							library.isRepeating
						)
						nowPlayingCache[libraryId] = nowPlaying
						return@let nowPlaying.toPromise()
					}

					promiseParsedFileStringList(savedTracksString)
						.then { files ->
							val nowPlaying = NowPlaying(
								if (files is List<*>) files as List<ServiceFile> else ArrayList(files),
								library.nowPlayingId,
								library.nowPlayingProgress,
								library.isRepeating
							)
							nowPlayingCache[libraryId] = nowPlaying
							nowPlaying
						}
				}.keepPromise()
			}

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
        if (libraryId < 0) return promiseNowPlaying().eventually { updateNowPlaying(nowPlaying) }
		nowPlayingCache[libraryId] = nowPlaying
        libraryProvider.library
            .then { library ->
				library?.apply {
					setNowPlayingId(nowPlaying.playlistPosition)
					setNowPlayingProgress(nowPlaying.filePosition)
					setRepeating(nowPlaying.isRepeating)
					promiseSerializedFileStringList(nowPlaying.playlist)
						.eventually { serializedPlaylist ->
							setSavedTracksString(serializedPlaylist)
							libraryRepository.saveLibrary(this)
						}
				}
			}
		return nowPlaying.toPromise()
    }
}
