package com.lasthopesoftware.bluewater.client.browsing.items.menu

import com.lasthopesoftware.bluewater.shared.messages.ApplicationMessage

enum class ActivityLaunching : ApplicationMessage {
	LAUNCHING,
	FINISHED,
	HALTED
}
