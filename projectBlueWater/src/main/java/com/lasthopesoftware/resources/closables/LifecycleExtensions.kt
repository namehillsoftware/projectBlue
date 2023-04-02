package com.lasthopesoftware.resources.closables

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

fun <T : AutoCloseable> LifecycleOwner.lazyScoped(factory: () -> T): Lazy<T> = lazy {
	factory().also { LifecycleAutoCloser(this, it) }
}

private class LifecycleAutoCloser(private val owner: LifecycleOwner, private val closeable: AutoCloseable) : LifecycleEventObserver {
	init {
		owner.lifecycle.addObserver(this)
	}

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (source !== owner || event != Lifecycle.Event.ON_DESTROY) return

		try {
			closeable.close()
		} finally {
			owner.lifecycle.removeObserver(this)
		}
	}
}
