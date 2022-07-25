package com.lasthopesoftware.bluewater.client.stored.sync.receivers

import android.content.Context
import com.lasthopesoftware.bluewater.client.stored.library.items.SyncItemStateChanged
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler

class SyncItemStateChangedListener(private val context: Context) : (SyncItemStateChanged) -> Unit {
	override fun invoke(p1: SyncItemStateChanged) {
		SyncScheduler.syncImmediately(context)
	}
}
