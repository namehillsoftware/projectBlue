package com.lasthopesoftware.resources.closables

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@MainThread
fun <T : AutoCloseable> LifecycleOwner.lazyScoped(factory: () -> T): Lazy<T> = LifecycleAutoCloser(this, lazy(factory))

private class LifecycleAutoCloser<T : AutoCloseable>(private val owner: LifecycleOwner, private val innerLazy: Lazy<T>) : LifecycleEventObserver, Lazy<T> by innerLazy {
	init {
		owner.lifecycle.addObserver(this)
	}

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (source !== owner || event != Lifecycle.Event.ON_DESTROY) return

		try {
			if (innerLazy.isInitialized())
				innerLazy.value.close()
		} finally {
			owner.lifecycle.removeObserver(this)
		}
	}
}
