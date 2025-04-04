package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseParsedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities.promiseSerializedFileStringList
import com.lasthopesoftware.bluewater.client.browsing.library.access.ManageLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingRepository(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val libraryManager: ManageLibraries
) : ManageNowPlayingState {

	private val serializedTracksCache by lazy { LruPromiseCache<List<ServiceFile>, String>(1) }

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> =
		selectedLibraryId
			.promiseSelectedLibraryId()
			.eventually { it?.let(::promiseNowPlaying).keepPromise() }

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> =
		libraryManager
			.promiseNowPlayingValues(libraryId)
			.eventually { library ->
				library?.run {
					savedTracksString
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
							nowPlaying
						}
						?: NowPlaying(
							libraryId,
							emptyList(),
							nowPlayingId,
							nowPlayingProgress,
							isRepeating
						).toPromise()
				}.keepPromise()
			}

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		val libraryId = nowPlaying.libraryId

		return Promise.Proxy { cp ->
			serializedTracksCache
				.getOrAdd(nowPlaying.playlist, ::promiseSerializedFileStringList)
				.also(cp::doCancel)
				.eventually { serializedPlaylist ->
					nowPlaying.run {
						libraryManager
							.updateNowPlaying(
								LibraryNowPlayingValues(
									id = libraryId.id,
									nowPlayingId = playlistPosition,
									nowPlayingProgress = filePosition,
									savedTracksString = serializedPlaylist,
									isRepeating = isRepeating
								)
							)
							.also(cp::doCancel)
					}
				}
				.then { _ -> nowPlaying }
		}
	}
}
