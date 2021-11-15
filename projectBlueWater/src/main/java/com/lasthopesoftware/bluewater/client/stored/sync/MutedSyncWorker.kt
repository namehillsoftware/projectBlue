package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.WorkerParameters

@RequiresApi(Build.VERSION_CODES.S)
class MutedSyncWorker(context: Context, workerParams: WorkerParameters) : SyncWorker(context, workerParams) {
	companion object {
		private val foregroundStates by lazy { arrayOf(Lifecycle.State.STARTED, Lifecycle.State.RESUMED) }
	}

	private var isForeground = false

	override fun notify(notificationText: String?) {
		if (isForeground || foregroundStates.contains(ProcessLifecycleOwner.get().lifecycle.currentState)) {
			isForeground = true
			super.notify(notificationText)
		}
	}
}
