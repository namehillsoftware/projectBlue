package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.resources.closables.ResettableCloseable
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReusablePlaylistFileViewModel(
	private val controlPlaybackService: ControlPlaybackService,
	private val applicationNavigation: NavigateApplication,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
	private val viewFileItem: ViewFileItem
) : ViewPlaylistFileItem, HiddenListItemMenu, ResettableCloseable {

	@Volatile
	private var activePositionedFile: PositionedFile? = null

	@Volatile
	private var associatedPlaylist = emptyList<ServiceFile>()

	private val mutableIsMenuShown = MutableStateFlow(false)

	override val artist = viewFileItem.artist
	override val title = viewFileItem.title
	override val isMenuShown = mutableIsMenuShown.asStateFlow()

	override fun promiseUpdate(associatedPlaylist: List<ServiceFile>, position: Int): Promise<Unit> {
		val serviceFile = associatedPlaylist[position]
		activePositionedFile = PositionedFile(position, serviceFile)
		this.associatedPlaylist = associatedPlaylist

		return viewFileItem.promiseUpdate(serviceFile)
	}

	override fun showMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = false, update = true)
			.also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuShown(this))
			}

	override fun hideMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = true, update = false)
			.also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuHidden(this))
			}

	override fun addToNowPlaying() {
		activePositionedFile?.serviceFile?.apply(controlPlaybackService::addToPlaylist)
		hideMenu()
	}

	override fun viewFileDetails() {
		activePositionedFile?.apply {
			applicationNavigation.viewFileDetails(associatedPlaylist, playlistPosition)
		}
		hideMenu()
	}

	override fun close() {
		viewFileItem.close()
		reset()
	}

	override fun reset() {
		viewFileItem.reset()
		resetState()
	}

	private fun resetState() {
		hideMenu()
	}
}
