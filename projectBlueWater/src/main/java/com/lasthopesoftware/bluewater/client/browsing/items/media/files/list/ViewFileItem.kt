package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.StateFlow

interface ViewFileItem : HiddenListItemMenu {
	val artist: StateFlow<String>
	val title: StateFlow<String>

	fun promiseUpdate(serviceFile: ServiceFile): Promise<Unit>
	fun addToNowPlaying()
	fun viewFileDetails()
	fun reset()
}
