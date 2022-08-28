package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingFileProvider private constructor(private val nowPlayingRepository: MaintainNowPlayingState) :
	ProvideNowPlayingFiles {
	override val nowPlayingFile: Promise<ServiceFile?>
		get() = nowPlayingRepository
			.promiseNowPlaying()
			.then { np -> np?.playingFile?.serviceFile }

	companion object {
		fun fromActiveLibrary(context: Context): Promise<NowPlayingFileProvider?> {
			val libraryRepository = LibraryRepository(context)
			val applicationSettingsRepository = context.getApplicationSettingsRepository()
			val selectedBrowserLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(applicationSettingsRepository)
			return selectedBrowserLibraryIdentifierProvider.selectedLibraryId
				.then { l ->
					l?.let {
						NowPlayingFileProvider(
							NowPlayingRepository(
								SpecificLibraryProvider(it, libraryRepository),
								libraryRepository
							)
						)
					}
				}
		}
	}
}
