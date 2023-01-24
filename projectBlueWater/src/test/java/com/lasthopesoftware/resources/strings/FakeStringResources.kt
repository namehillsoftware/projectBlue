package com.lasthopesoftware.resources.strings

class FakeStringResources : GetStringResources {
	override val loading: String
		get() = ""
	override val unknownArtist: String
		get() = ""
	override val unknownTrack: String
		get() = ""
	override val defaultNowPlayingTrackTitle: String
		get() = ""
	override val defaultNowPlayingArtist: String
		get() = ""
	override val aboutTitle: String
		get() = ""
	override val connecting: String
		get() = ""
	override val gettingLibrary: String
		get() = ""
	override val gettingLibraryFailed: String
		get() = ""
	override val sendingWakeSignal: String
		get() = ""
	override val connectingToServerLibrary: String
		get() = ""
	override val errorConnectingTryAgain: String
		get() = ""
	override val connected: String
		get() = ""
}
