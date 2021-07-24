package com.lasthopesoftware.bluewater.client.servers.list.listeners

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.IBrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.startNewConnection

class SelectServerOnClickListener(
	private val library: Library,
	private val browserLibrarySelection: IBrowserLibrarySelection
) : View.OnClickListener {
	override fun onClick(v: View) {
		browserLibrarySelection.selectBrowserLibrary(library.libraryId)
		startNewConnection(v.context)
	}
}
