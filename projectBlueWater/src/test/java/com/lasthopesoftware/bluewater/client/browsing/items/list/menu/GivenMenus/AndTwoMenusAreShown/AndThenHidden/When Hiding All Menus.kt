package com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus.AndTwoMenusAreShown.AndThenHidden

import com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus.FakeMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Hiding All Menus` {

	private val arrangedServices by lazy {
		val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()
		Pair(recordingMessageBus, ItemListMenuBackPressedHandler(recordingMessageBus))
	}
	private var wereAnyMenusHidden = false

	@BeforeAll
	fun act() {
		val (messageBus, handler) = arrangedServices
		val firstMenu = FakeMenu()
		val secondMenu = FakeMenu()
		messageBus.sendMessage(ItemListMenuMessage.MenuShown(firstMenu))
		messageBus.sendMessage(ItemListMenuMessage.MenuShown(secondMenu))
		messageBus.sendMessage(ItemListMenuMessage.MenuHidden(firstMenu))
		messageBus.sendMessage(ItemListMenuMessage.MenuHidden(secondMenu))
		wereAnyMenusHidden = handler.hideAllMenus()
	}

	@Test
	fun `then no menus were hidden`() {
		assertThat(wereAnyMenusHidden).isFalse
	}
}
