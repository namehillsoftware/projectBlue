package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

class CachedSelectedLibraryIdProvider(private val inner: ProvideSelectedLibraryId, private val selectedLibraryIdCache: HoldSelectedLibraryId)
	: ProvideSelectedLibraryId
{
	companion object {
		fun Context.getCachedSelectedLibraryIdProvider() = CachedSelectedLibraryIdProvider(
			SelectedLibraryIdProvider(getApplicationSettingsRepository()),
			SelectedLibraryIdCache
		)
	}

	private object SelectedLibraryIdCache : HoldSelectedLibraryId, (BrowserLibrarySelection.LibraryChosenMessage) -> Unit {
		private val cachedPromisedLibrary = AtomicReference<Promise<LibraryId?>?>(null)

		init {
		    ApplicationMessageBus.getApplicationMessageBus().registerReceiver(this)
		}

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
			cachedPromisedLibrary.compareAndSet(cachedPromisedLibrary.get(), message.chosenLibraryId.toPromise())
		}
	}

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = selectedLibraryIdCache.getOrCache(inner::promiseSelectedLibraryId)
}
