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
}
