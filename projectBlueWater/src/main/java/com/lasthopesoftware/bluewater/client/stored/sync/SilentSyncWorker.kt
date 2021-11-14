package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class SilentSyncWorker(context: Context, workerParams: WorkerParameters) : SyncWorker(context, workerParams) {
	override fun continueWork(cancellationProxy: CancellationProxy) = Unit.toPromise()

	override fun notify(notificationText: String?) {}
}
