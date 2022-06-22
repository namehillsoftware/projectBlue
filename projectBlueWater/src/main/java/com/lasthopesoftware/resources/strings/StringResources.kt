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
}
