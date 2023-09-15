package com.lasthopesoftware.resources.strings

import android.content.Context
import androidx.annotation.StringRes
import com.lasthopesoftware.bluewater.R

class StringResources(private val context: Context): GetStringResources {
	override val loading by lazyString(R.string.lbl_loading)
	override val unknownArtist by lazyString(R.string.unknown_artist)
	override val unknownTrack by lazyString(R.string.unknown_track)
	override val defaultNowPlayingTrackTitle by lazyString(R.string.lbl_song_title)
	override val defaultNowPlayingArtist by lazyString(R.string.lbl_song_artist)
	override val aboutTitle by lazy { context
		.getString(R.string.title_activity_about)
		.format(context.getString(R.string.app_name)) }
	override val connecting by lazyString(R.string.lbl_connecting)
	override val gettingLibrary by lazyString(R.string.lbl_getting_library_details)
	override val gettingLibraryFailed by lazyString(R.string.lbl_please_connect_to_valid_server)
	override val sendingWakeSignal by lazyString(R.string.sending_wake_signal)
	override val connectingToServerLibrary by lazyString(R.string.lbl_connecting_to_server_library)
	override val errorConnectingTryAgain by lazyString(R.string.lbl_error_connecting_try_again)
	override val connected by lazyString(R.string.lbl_connected)
	override val removeServer by lazyString(R.string.remove_server)
	override val connectingToServerTitle by lazyString(R.string.title_svc_connecting_to_server)
	override val permissionsNeeded by lazyString(R.string.permissions_needed)
	override val permissionsNeededLaunchSettings by lazyString(R.string.permissions_needed_launch_settings)
	override val saveAndConnect by lazyString(R.string.save_and_connect)
	override val connect by lazyString(R.string.lbl_connect)
	override val clear by lazyString(R.string.clear)
	override val set by lazyString(R.string.set)
	override val change by lazyString(R.string.change)

	private fun lazyString(@StringRes stringResourceId: Int) = lazy { context.getString(stringResourceId) }
}
