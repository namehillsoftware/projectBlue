package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.InteractionState
import com.lasthopesoftware.resources.closables.ResettableCloseable
import com.namehillsoftware.handoff.promises.Promise

interface ViewFileItem : ResettableCloseable {
	val artist: InteractionState<String>
	val title: InteractionState<String>

	fun promiseUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit>
}
