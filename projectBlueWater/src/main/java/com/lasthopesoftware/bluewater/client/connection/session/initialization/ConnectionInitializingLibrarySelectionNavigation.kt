package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionInitializingLibrarySelectionNavigation(
	private val inner: NavigateApplication,
	private val connectionStatusViewModel: ConnectionStatusViewModel
) : NavigateApplication by inner {
	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> =
		connectionStatusViewModel
			.ensureConnectionIsWorking(libraryId)
			.eventually {
				if (it) inner.viewLibrary(libraryId)
				else Unit.toPromise()
			}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> =
		connectionStatusViewModel
			.ensureConnectionIsWorking(libraryId)
			.eventually {
				if (it) inner.viewItem(libraryId, item)
				else Unit.toPromise()
			}

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> =
		connectionStatusViewModel
			.ensureConnectionIsWorking(libraryId)
			.eventually {
				if (it) inner.launchSearch(libraryId)
				else Unit.toPromise()
			}

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> =
		connectionStatusViewModel
			.ensureConnectionIsWorking(libraryId)
			.eventually {
				if (it) inner.viewFileDetails(libraryId, playlist, position)
				else Unit.toPromise()
			}
}
