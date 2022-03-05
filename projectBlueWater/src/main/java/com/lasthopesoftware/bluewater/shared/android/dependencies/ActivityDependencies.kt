package com.lasthopesoftware.bluewater.shared.android.dependencies

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap

object ActivityDependencies : LifecycleEventObserver {
	private val lifecycleDependencySyncs = ConcurrentHashMap<LifecycleOwner, Any>()
	private val lifecycleDependencyFactories = ConcurrentHashMap<LifecycleOwner, () -> AutoCloseable>()
	private val lifecycleDependencies = ConcurrentHashMap<LifecycleOwner, AutoCloseable>()

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (event == ON_DESTROY) lifecycleDependencies.remove(source)?.close()
	}

	fun LifecycleOwner.getDependenciesAsAny() = synchronized(lifecycleDependencySyncs.getOrPut(this) { Any() }) {
		val dependencies = lifecycleDependencies[this]
		if (dependencies != null) dependencies
		else {
			val newLifecycle = lifecycleDependencyFactories[this]?.invoke()
			if (newLifecycle == null) null
			else {
				lifecycleDependencies[this] = newLifecycle
				newLifecycle
			}
		}
	}

	inline fun <reified T : AutoCloseable> LifecycleOwner.getDependencies() = getDependenciesAsAny() as? T

	fun <T: AutoCloseable> LifecycleOwner.registerDependencies(dependencyFactory: () -> T) {
		lifecycleDependencyFactories[this] = dependencyFactory
	}
}
