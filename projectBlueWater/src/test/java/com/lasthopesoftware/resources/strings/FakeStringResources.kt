package com.lasthopesoftware.resources.strings

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
    override val subsonic: String = "",
) : GetStringResources
