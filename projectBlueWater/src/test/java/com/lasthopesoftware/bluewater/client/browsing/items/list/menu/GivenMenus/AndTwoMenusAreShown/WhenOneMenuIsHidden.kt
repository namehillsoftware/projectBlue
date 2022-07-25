package com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus.AndTwoMenusAreShown

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuViewModel
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

private val arrangedServices by lazy {
	val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()
	Pair(recordingMessageBus, ItemListMenuViewModel(recordingMessageBus))
}

class WhenOneMenuIsHidden {

	companion object {
		@BeforeClass
		@JvmStatic
		fun act() {
			val (messageBus, _) = arrangedServices
			val firstMenu = mockk<HiddenListItemMenu>()
			messageBus.sendMessage(ItemListMenuMessage.MenuShown(firstMenu))
			messageBus.sendMessage(ItemListMenuMessage.MenuShown(mockk()))
			messageBus.sendMessage(ItemListMenuMessage.MenuHidden(firstMenu))
		}
	}

	@Test
	fun `then isAnyMenuShown is correct`() {
		assertThat(arrangedServices.second.isAnyMenuShown.value).isTrue
	}
}
