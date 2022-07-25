package com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus

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

class WhenAMenuShownMessageIsSent {
	companion object {
		@BeforeClass
		@JvmStatic
		fun act() {
			val (messageBus, _) = arrangedServices
			messageBus.sendMessage(ItemListMenuMessage.MenuShown(mockk()))
		}
	}

	@Test
	fun `then isAnyMenuShown is correct`() {
		assertThat(arrangedServices.second.isAnyMenuShown.value).isTrue
	}
}
