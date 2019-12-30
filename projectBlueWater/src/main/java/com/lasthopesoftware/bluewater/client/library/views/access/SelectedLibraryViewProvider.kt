package com.lasthopesoftware.bluewater.client.library.views.access

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.library.repository.Library
import com.lasthopesoftware.bluewater.client.library.views.DownloadViewItem
import com.lasthopesoftware.bluewater.client.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryViewProvider(
	private val selectedLibrary: ISelectedBrowserLibraryProvider,
	private val libraryViews: ProvideLibraryViews,
	private val libraryStorage: ILibraryStorage) : ProvideSelectedLibraryView {

	override fun promiseSelectedOrDefaultView(): Promise<ViewItem?> {
		val promisedSelectedLibrary = selectedLibrary.browserLibrary
		return libraryViews.promiseLibraryViews().eventually { views ->
			if (views.isNotEmpty()) {
				promisedSelectedLibrary.eventually { library ->
					when {
						library.selectedViewType == Library.ViewType.DownloadView -> Promise(DownloadViewItem() as ViewItem)
						library.selectedView > 0 -> Promise(views.first { it.key == library.selectedView })
						else -> {
							val selectedView = views.first()
							library.selectedView = selectedView.key
							library.selectedViewType = Library.ViewType.StandardServerView
							libraryStorage.saveLibrary(library).then { selectedView }
						}
					}
				}
			}
			else {
				Promise.empty()
			}
		}
	}
}
