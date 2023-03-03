package com.lasthopesoftware.bluewater.shared.android.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.resources.closables.ResettableCloseable
import java.util.concurrent.ConcurrentLinkedQueue

abstract class PooledCloseablesViewModel<C : ResettableCloseable> : ViewModel() {
	private val allViewModels = ConcurrentLinkedQueue<PooledCloseable>()
	private val viewModelPool = ConcurrentLinkedQueue<PooledCloseable>()

	override fun onCleared() {
		allViewModels.forEach { it.close() }
		super.onCleared()
	}

	fun getViewModel(): C = viewModelPool.poll()?.innerCloseable
		?: PooledCloseable(getNewCloseable()).also(allViewModels::offer).innerCloseable

	protected abstract fun getNewCloseable(): C

	private inner class PooledCloseable(val innerCloseable: C) : ResettableCloseable {
		override fun reset() {
			innerCloseable.reset()
			viewModelPool.offer(this)
		}

		override fun close() {
			innerCloseable.reset()
			viewModelPool.offer(this)
		}
	}
}
