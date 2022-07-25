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

class WhenBothMenusAreHidden {

	companion object {
		@BeforeClass
		@JvmStatic
		fun act() {
			val (messageBus, _) = arrangedServices
			val firstMenu = mockk<HiddenListItemMenu>()
			val secondMenu = mockk<HiddenListItemMenu>()
			messageBus.sendMessage(ItemListMenuMessage.MenuShown(firstMenu))
			messageBus.sendMessage(ItemListMenuMessage.MenuShown(secondMenu))
			messageBus.sendMessage(ItemListMenuMessage.MenuHidden(firstMenu))
			messageBus.sendMessage(ItemListMenuMessage.MenuHidden(secondMenu))
		}
	}

	@Test
	fun `then isAnyMenuShown is correct`() {
		assertThat(arrangedServices.second.isAnyMenuShown.value).isFalse
	}
}
