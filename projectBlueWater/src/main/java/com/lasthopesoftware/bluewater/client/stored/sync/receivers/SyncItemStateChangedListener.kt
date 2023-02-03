package com.lasthopesoftware.bluewater.client.stored.sync.receivers

import com.lasthopesoftware.bluewater.client.stored.library.items.SyncItemStateChanged
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs

class SyncItemStateChangedListener(private val syncScheduler: ScheduleSyncs) : (SyncItemStateChanged) -> Unit {
	override fun invoke(p1: SyncItemStateChanged) {
		syncScheduler.syncImmediately()
	}
}
