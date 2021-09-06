package com.lasthopesoftware.bluewater.client.playback.view.nowplaying

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingFileProvider private constructor(private val nowPlayingRepository: INowPlayingRepository) : INowPlayingFileProvider {
	override val nowPlayingFile: Promise<ServiceFile>
		get() = nowPlayingRepository
			.nowPlaying
			.then { np -> if (np.playlist.size > 0) np.playlist[np.playlistPosition] else null }

	companion object {
		@JvmStatic
		fun fromActiveLibrary(context: Context): Promise<NowPlayingFileProvider?> {
			val libraryRepository = LibraryRepository(context)
			val applicationSettingsRepository = context.getApplicationSettings()
			val selectedBrowserLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(applicationSettingsRepository)
			return selectedBrowserLibraryIdentifierProvider.selectedLibraryId
				.then { l ->
					l?.let {
						NowPlayingFileProvider(
							NowPlayingRepository(
								SpecificLibraryProvider(it, libraryRepository),
								libraryRepository))
					}
				}
		}
	}
}
