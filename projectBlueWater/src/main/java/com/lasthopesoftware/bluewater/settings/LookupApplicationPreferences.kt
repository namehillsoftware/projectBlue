package com.lasthopesoftware.bluewater.settings

import com.namehillsoftware.handoff.promises.Promise

interface LookupApplicationPreferences {
	fun promiseApplicationPreferences(): Promise<ApplicationPreferences>
}
