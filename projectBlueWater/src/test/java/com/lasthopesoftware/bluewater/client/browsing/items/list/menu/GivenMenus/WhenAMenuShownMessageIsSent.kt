package com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenAMenuShownMessageIsSent {
	private val arrangedServices by lazy {
		val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()
		Pair(recordingMessageBus, ItemListMenuBackPressedHandler(recordingMessageBus))
	}

	@BeforeAll
	fun act() {
		val (messageBus, _) = arrangedServices
		messageBus.sendMessage(ItemListMenuMessage.MenuShown(mockk()))
	}

	@Test
	fun `then isAnyMenuShown is correct`() {
		assertThat(arrangedServices.second.isEnabled).isTrue
	}
}
