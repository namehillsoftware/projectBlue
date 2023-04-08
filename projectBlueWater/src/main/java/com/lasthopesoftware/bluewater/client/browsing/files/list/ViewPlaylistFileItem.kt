package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.resources.closables.ResettableCloseable
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.StateFlow

interface ViewPlaylistFileItem : HiddenListItemMenu, ResettableCloseable {
	val artist: StateFlow<String>
	val title: StateFlow<String>

	fun promiseUpdate(serviceFile: ServiceFile): Promise<Unit>
}
