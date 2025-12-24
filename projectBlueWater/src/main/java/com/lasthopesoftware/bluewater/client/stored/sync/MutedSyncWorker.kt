package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.WorkerParameters
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker

@RequiresApi(Build.VERSION_CODES.S)
class MutedSyncWorker(context: Context, workerParams: WorkerParameters) : SyncWorker(context, workerParams) {
	companion object {
		private val foregroundStates by lazy { arrayOf(Lifecycle.State.STARTED, Lifecycle.State.RESUMED) }
	}

	private val permissionsChecker by lazy { OsPermissionsChecker(context) }

	override fun notify(notificationText: String?) {
		if (permissionsChecker.isForegroundDataServicePermissionNotGranted) return

		if (foregroundStates.contains(ProcessLifecycleOwner.get().lifecycle.currentState)) {
			super.notify(notificationText)
		}
	}
}
