package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.updateIfDifferent
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

class SelectedLibraryIdCache(
    messageBus: RegisterForApplicationMessages,
) : HoldSelectedLibraryId, AutoCloseable, (BrowserLibrarySelection.LibraryChosenMessage) -> Unit {
    val cachedPromisedLibrary = AtomicReference<Promise<LibraryId?>?>(null)

    val subscription = messageBus.registerReceiver(this)

    override fun getOrCache(factory: () -> Promise<LibraryId?>): Promise<LibraryId?> =
        with(cachedPromisedLibrary) {
            var cachedPromise = get()
            while (cachedPromise == null) {
                set(factory())
                cachedPromise = get()
            }

            cachedPromise
        }

    override fun invoke(message: BrowserLibrarySelection.LibraryChosenMessage) {
        cachedPromisedLibrary.updateIfDifferent( message.chosenLibraryId.toPromise())
    }

    override fun close() {
        subscription.close()
    }
}
