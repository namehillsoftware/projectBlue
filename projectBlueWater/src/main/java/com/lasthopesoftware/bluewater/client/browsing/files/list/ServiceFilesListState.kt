package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.observables.InteractionState

interface ServiceFilesListState {
	val files: InteractionState<List<ServiceFile>>
}
