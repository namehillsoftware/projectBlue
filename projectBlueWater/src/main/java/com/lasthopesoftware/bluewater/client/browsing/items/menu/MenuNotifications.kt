package com.lasthopesoftware.bluewater.client.browsing.items.menu

import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.cls

object MenuNotifications {
	private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<MenuNotifications>()) }

	val launchingActivity by lazy { magicPropertyBuilder.buildProperty("launchingActivity") }
	val launchingActivityFinished by lazy { magicPropertyBuilder.buildProperty("launchingActivityFinished") }
	val launchingActivityHalted by lazy { magicPropertyBuilder.buildProperty("launchingActivityHalted") }
}
