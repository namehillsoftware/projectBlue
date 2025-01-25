package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.content.Context
import androidx.startup.Initializer
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies

class LiveNowPlayingLookupInitializer : Initializer<LiveNowPlayingLookup> {
	override fun create(context: Context): LiveNowPlayingLookup = with (context.applicationDependencies) {
		LiveNowPlayingLookup(
			selectedLibraryIdProvider,
			NowPlayingRepository(
				selectedLibraryIdProvider,
				libraryProvider,
				libraryStorage,
				InMemoryNowPlayingState,
			),
			registerForApplicationMessages,
		)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
