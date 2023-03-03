package com.lasthopesoftware.bluewater.client.stored.sync

import androidx.work.Constraints
import androidx.work.Operation
import androidx.work.WorkInfo
import com.namehillsoftware.handoff.promises.Promise

interface ScheduleSyncs {
	fun syncImmediately(): Promise<Operation>
	fun scheduleSync(): Promise<Operation>
	fun cancelSync(): Promise<Unit>
	fun constraints(): Promise<Constraints>
	fun promiseIsSyncing(): Promise<Boolean>
	fun promiseIsScheduled(): Promise<Boolean>
	fun promiseWorkInfos(): Promise<List<WorkInfo>>
}
