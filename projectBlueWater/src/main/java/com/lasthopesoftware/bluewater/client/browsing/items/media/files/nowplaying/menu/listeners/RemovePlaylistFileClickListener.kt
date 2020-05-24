package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.vedsoft.futures.runnables.OneParameterAction

/**
 * Created by david on 11/8/15.
 */
class RemovePlaylistFileClickListener // TODO Add event and remove interdepency on NowPlayingFileListAdapter adapter
(parent: NotifyOnFlipViewAnimator, private val position: Int, private val onPlaylistFileRemoved: OneParameterAction<Int>?) : AbstractMenuClickHandler(parent) {
	override fun onClick(view: View) {
		PlaybackService.removeFileAtPositionFromPlaylist(view.context, position)
		onPlaylistFileRemoved?.runWith(position)
		super.onClick(view)
	}
}
