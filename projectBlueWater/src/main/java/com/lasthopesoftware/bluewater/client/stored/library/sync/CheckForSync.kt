package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.namehillsoftware.handoff.promises.Promise

interface CheckForSync {
    fun promiseIsSyncNeeded(): Promise<Boolean>
}
