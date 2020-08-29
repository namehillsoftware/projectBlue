package com.lasthopesoftware.bluewater.client.browsing.items.media.files

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService

class FilePlayClickListener(parent: NotifyOnFlipViewAnimator, private val position: Int, private val serviceFiles: Collection<ServiceFile>)
	: AbstractMenuClickHandler(parent) {
	override fun onClick(v: View) {
		val context = v.context
		FileStringListUtilities
			.promiseSerializedFileStringList(serviceFiles)
			.then { fileStringList -> PlaybackService.launchMusicService(context, position, fileStringList) }
		super.onClick(v)
	}
}
