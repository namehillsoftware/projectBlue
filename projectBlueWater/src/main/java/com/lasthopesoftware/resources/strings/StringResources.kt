package com.lasthopesoftware.resources.strings

import android.content.Context
import com.lasthopesoftware.bluewater.R

class StringResources(context: Context): GetStringResources {
	override val loading by lazy { context.getString(R.string.lbl_loading) }
	override val unknownArtist by lazy { context.getString(R.string.unknown_artist )}
	override val unknownTrack by lazy { context.getString(R.string.unknown_track) }
	override val defaultNowPlayingTrackTitle by lazy { context.getString(R.string.lbl_song_title) }
	override val defaultNowPlayingArtist by lazy { context.getString(R.string.lbl_song_artist) }
	override val aboutTitle by lazy { context
		.getString(R.string.title_activity_about)
		.format(context.getString(R.string.app_name)) }
	override val connecting by lazy { context.getString(R.string.lbl_connecting) }
	override val gettingLibrary by lazy { context.getString(R.string.lbl_getting_library_details) }
	override val gettingLibraryFailed by lazy { context.getString(R.string.lbl_please_connect_to_valid_server) }
	override val sendingWakeSignal by lazy { context.getString(R.string.sending_wake_signal) }
	override val connectingToServerLibrary by lazy { context.getString(R.string.lbl_connecting_to_server_library) }
	override val errorConnectingTryAgain by lazy { context.getString(R.string.lbl_error_connecting_try_again) }
	override val connected by lazy { context.getString(R.string.lbl_connected) }
	override val removeServer by lazy { context.getString(R.string.removeServer) }
	override val connectingToServerTitle by lazy { context.getString(R.string.title_svc_connecting_to_server) }
}
