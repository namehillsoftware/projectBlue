package com.lasthopesoftware.bluewater.client.stored.sync

import io.reactivex.rxjava3.core.Completable

interface SynchronizeStoredFiles {
    fun streamFileSynchronization(): Completable?
}
