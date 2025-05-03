package com.lasthopesoftware.resources.string

import com.lasthopesoftware.resources.strings.GetStringResources

class FakeStringResources(
	override val loading: String = "",
	override val unknownArtist: String = "",
	override val unknownTrack: String = "",
	override val defaultNowPlayingTrackTitle: String = "",
	override val defaultNowPlayingArtist: String = "",
	override val aboutTitle: String = "",
	override val connecting: String = "",
	override val gettingLibrary: String = "",
	override val gettingLibraryFailed: String = "",
	override val sendingWakeSignal: String = "",
	override val connectingToServerLibrary: String = "",
	override val errorConnectingTryAgain: String = "",
	override val connected: String = "",
	override val play: String = "",
	override val pause: String = "",
	override val next: String = "",
	override val previous: String = "",
) : GetStringResources
