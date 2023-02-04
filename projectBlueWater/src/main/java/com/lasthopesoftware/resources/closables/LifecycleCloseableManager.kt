package com.lasthopesoftware.resources.closables

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleCloseableManager(private val lifecycleOwner: LifecycleOwner) : ManageCloseables, LifecycleEventObserver {
	private val autoCloseableManager = AutoCloseableManager()

	init {
	    lifecycleOwner.lifecycle.addObserver(this)
	}

	override fun manage(closeable: AutoCloseable) {
		autoCloseableManager.manage(closeable)
	}

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (source !== lifecycleOwner || event != Lifecycle.Event.ON_DESTROY) return

		try {
			autoCloseableManager.close()
		} finally {
		    lifecycleOwner.lifecycle.removeObserver(this)
		}
	}
}
