package com.lasthopesoftware.bluewater.client.playback.view.nowplaying

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingFileProvider private constructor(private val nowPlayingRepository: INowPlayingRepository) : INowPlayingFileProvider {
	override val nowPlayingFile: Promise<ServiceFile>
		get() = nowPlayingRepository
			.nowPlaying
			.then { np -> if (np.playlist.size > 0) np.playlist[np.playlistPosition] else null }

	companion object {
		@JvmStatic
		fun fromActiveLibrary(context: Context): NowPlayingFileProvider? {
			val libraryRepository = LibraryRepository(context)
			val selectedBrowserLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(context)
			val libraryId = selectedBrowserLibraryIdentifierProvider.selectedLibraryId ?: return null

			return NowPlayingFileProvider(
				NowPlayingRepository(
					SpecificLibraryProvider(
						libraryId,
						libraryRepository),
					libraryRepository))
		}
	}
}
