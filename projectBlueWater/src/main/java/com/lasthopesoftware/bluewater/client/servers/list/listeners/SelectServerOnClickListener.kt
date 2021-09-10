package com.lasthopesoftware.bluewater.client.servers.list.listeners

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.startNewConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

class SelectServerOnClickListener(
	private val library: Library,
	private val browserLibrarySelection: SelectBrowserLibrary
) : View.OnClickListener {
	override fun onClick(v: View) {
		browserLibrarySelection.selectBrowserLibrary(library.libraryId)
			.eventually(LoopedInPromise.response({ startNewConnection(v.context) }, v.context))
	}
}
