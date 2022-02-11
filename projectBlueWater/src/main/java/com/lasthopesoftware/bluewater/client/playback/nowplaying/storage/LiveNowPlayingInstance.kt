package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection.Companion.chosenLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages

class LiveNowPlayingInstance(private val messageBus: RegisterForMessages, selectedLibraryIdentifierProvider: ProvideSelectedLibraryId, private val libraryProvider: ILibraryProvider, private val libraryStorage: ILibraryStorage) : BroadcastReceiver() {
	companion object {
		var nowPlayingLookupInstance: LiveNowPlayingLookup? = null
	}

	init {
	    selectedLibraryIdentifierProvider.selectedLibraryId.then {
	    	it?.also(::updateNowPlayingLookupInstance)
		}
	}

	override fun onReceive(context: Context?, intent: Intent?) {
		nowPlayingLookupInstance?.also(messageBus::unregisterReceiver)
		nowPlayingLookupInstance = null

		intent
			?.getIntExtra(chosenLibraryId, -1)
			?.takeIf { it > -1 }
			?.also { updateNowPlayingLookupInstance(LibraryId(it)) }
	}

	private fun updateNowPlayingLookupInstance(libraryId: LibraryId) {
		LiveNowPlayingLookup(
			NowPlayingRepository(
				SpecificLibraryProvider(libraryId, libraryProvider),
				libraryStorage
			)
		).also {
			nowPlayingLookupInstance = it
			messageBus.registerReceiver(it, IntentFilter(TrackPositionBroadcaster.trackPositionUpdate))
		}
	}
}
