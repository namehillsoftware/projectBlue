package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.DownloadViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.SearchViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryViewProvider(
	private val selectedLibrary: ISelectedBrowserLibraryProvider,
	private val libraryViews: ProvideLibraryViews,
	private val libraryStorage: ILibraryStorage) : ProvideSelectedLibraryView {

	override fun promiseSelectedOrDefaultView(): Promise<ViewItem?> =
		selectedLibrary.browserLibrary.eventually { library ->
			libraryViews.promiseLibraryViews(library.libraryId).eventually<ViewItem> { views ->
				if (views.isNotEmpty()) {
					when (library.selectedViewType) {
						Library.ViewType.DownloadView -> Promise(DownloadViewItem() as ViewItem)
						Library.ViewType.SearchView -> Promise(SearchViewItem() as ViewItem)
						else -> {
							if (library.selectedView > 0) Promise(views.first { it.key == library.selectedView })
							else {
								val selectedView = views.first()
								library
									.setSelectedView(selectedView.key)
									.setSelectedViewType(Library.ViewType.StandardServerView)
								libraryStorage.saveLibrary(library).then { selectedView }
							}
						}
					}
				}
				else Promise.empty()
			}
		}
}
