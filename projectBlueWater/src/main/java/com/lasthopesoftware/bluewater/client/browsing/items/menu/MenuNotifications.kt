package com.lasthopesoftware.bluewater.client.browsing.items.menu

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

enum class ActivityLaunching : ApplicationMessage {
	LAUNCHING,
	FINISHED,
	HALTED
}
