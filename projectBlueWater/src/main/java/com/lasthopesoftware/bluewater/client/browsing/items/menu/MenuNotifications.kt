package com.lasthopesoftware.bluewater.client.browsing.items.menu

import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder

object MenuNotifications {
	private val magicPropertyBuilder by lazy { MagicPropertyBuilder(MenuNotifications::class.java) }

	val launchingActivity by lazy { magicPropertyBuilder.buildProperty("launchingActivity") }
	val launchingActivityFinished by lazy { magicPropertyBuilder.buildProperty("launchingActivityFinished") }
}
