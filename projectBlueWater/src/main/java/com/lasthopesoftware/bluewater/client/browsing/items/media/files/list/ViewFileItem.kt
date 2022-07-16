package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.StateFlow

interface ViewFileItem : AutoCloseable {
	val artist: StateFlow<String>
	val title: StateFlow<String>
	val isMenuShown: StateFlow<Boolean>

	fun promiseUpdate(serviceFile: ServiceFile): Promise<Unit>
	fun showMenu()
	fun hideMenu(): Boolean
	fun addToNowPlaying()
	fun viewFileDetails()
}
