package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

/**
 * Created by david on 11/8/15.
 */
class RemovePlaylistFileClickListener(parent: NotifyOnFlipViewAnimator, private val position: Int) : AbstractMenuClickHandler(parent) {
	override fun onClick(view: View) {
		PlaybackService.removeFileAtPositionFromPlaylist(view.context, position)
		super.onClick(view)
	}
}
