package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import org.slf4j.LoggerFactory

class LoggingApplicationNavigation(private val inner: NavigateApplication) : NavigateApplication by inner {

	private val logger by lazy { LoggerFactory.getLogger(inner.javaClass) }

	override fun viewApplicationSettings(): Promise<Unit> {
		return logNavigation("viewApplicationSettings") { it.viewApplicationSettings() }
	}

	override fun viewHiddenSettings(): Promise<Unit> {
		return logNavigation("viewHiddenSettings") { it.viewHiddenSettings() }
	}

	override fun viewNewServerSettings(): Promise<Unit> {
		return logNavigation("viewNewServerSettings") { it.viewNewServerSettings() }
	}

	override fun viewActiveLibrary(): Promise<Unit> {
		return logNavigation("viewActiveLibrary") { it.viewActiveLibrary() }
	}

	override fun viewAllDownloads(): Promise<Unit> {
		return logNavigation("viewAllDownloads") { it.viewAllDownloads() }
	}

	override fun viewActiveDownloads(): Promise<Unit> {
		return logNavigation("viewActiveDownloads") { it.viewActiveDownloads() }
	}

	override fun searchActiveLibrary(searchQuery: String): Promise<Unit> {
		return logNavigation("searchActiveLibrary", searchQuery) { it, q -> it.searchActiveLibrary(q) }
	}

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> {
		return logNavigation("viewLibrary", libraryId) { it, id -> it.viewLibrary(id) }
	}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> {
		return logNavigation("viewItem", libraryId, item) { it, id, i -> it.viewItem(id, i) }
	}

	override fun viewServerSettings(libraryId: LibraryId): Promise<Unit> {
		return logNavigation("viewServerSettings", libraryId) { it, id -> it.viewServerSettings(id) }
	}

	override fun viewFileDetails(libraryId: LibraryId, searchQuery: String, positionedFile: PositionedFile): Promise<Unit> {
		return logNavigation("viewFileDetails", libraryId, searchQuery, positionedFile) { it, id, q, p -> it.viewFileDetails(id, q, p) }
	}

	override fun viewFileDetails(libraryId: LibraryId, item: IItem?, positionedFile: PositionedFile): Promise<Unit> {
		return logNavigation("viewFileDetails", libraryId, item, positionedFile) { it, id, i, p -> it.viewFileDetails(id, i, p) }
	}

	override fun viewNowPlayingFileDetails(libraryId: LibraryId, positionedFile: PositionedFile): Promise<Unit> {
		return logNavigation("viewNowPlayingFileDetails", libraryId, positionedFile) { it, id, p -> it.viewNowPlayingFileDetails(id, p) }
	}

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> {
		return logNavigation("launchSearch", libraryId) { it, id -> it.launchSearch(id) }
	}

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> {
		return logNavigation("search", libraryId, filePropertyFilter) { it, id, f -> it.search(id, f) }
	}

	override fun search(libraryId: LibraryId, searchQuery: String): Promise<Unit> {
		return logNavigation("search", libraryId, searchQuery) { it, id, q -> it.search(id, q) }
	}

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> {
		return logNavigation("viewNowPlaying", libraryId) { it, id -> it.viewNowPlaying(id) }
	}

	override fun viewActiveDownloads(libraryId: LibraryId): Promise<Unit> {
		return logNavigation("viewActiveDownloads", libraryId) { it, id -> it.viewActiveDownloads(id) }
	}

	private inline fun logNavigation(destination: String, action: (NavigateApplication) -> Promise<Unit>): Promise<Unit> {
		logger.info("Navigating to {}.", destination)
		return action(inner).must(ImmediateAction{
			logger.info("Navigation to {} finished.", destination)
		})
	}

	private inline fun <ParamOne> logNavigation(destination: String, param: ParamOne, action: (NavigateApplication, ParamOne) -> Promise<Unit>): Promise<Unit> {
		logger.info("Navigating to {}({}).", destination, param)
		return action(inner, param).must(ImmediateAction{
			logger.info("Navigation to {}({}) finished.", destination, param)
		})
	}

	private inline fun <ParamOne, ParamTwo> logNavigation(destination: String, paramOne: ParamOne, paramTwo: ParamTwo, action: (NavigateApplication, ParamOne, ParamTwo) -> Promise<Unit>): Promise<Unit> {
		logger.info("Navigating to {}({}, {}).", destination, paramOne, paramTwo)
		return action(inner, paramOne, paramTwo).must(ImmediateAction{
			logger.info("Navigation to {}({}, {}) finished.", destination, paramOne, paramTwo)
		})
	}

	private inline fun <ParamOne, ParamTwo, ParamThree> logNavigation(destination: String, paramOne: ParamOne, paramTwo: ParamTwo, paramThree: ParamThree, action: (NavigateApplication, ParamOne, ParamTwo, ParamThree) -> Promise<Unit>): Promise<Unit> {
		logger.info("Navigating to {}({}, {}, {}).", destination, paramOne, paramTwo, paramThree)
		return action(inner, paramOne, paramTwo, paramThree).must(ImmediateAction{
			logger.info("Navigation to {}({}, {}, {}) finished.", destination, paramOne, paramTwo, paramThree)
		})
	}
}
