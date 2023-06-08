package com.lasthopesoftware.bluewater.client.stored.sync

import io.reactivex.Completable

interface SynchronizeStoredFiles {
    fun streamFileSynchronization(): Completable?
}
