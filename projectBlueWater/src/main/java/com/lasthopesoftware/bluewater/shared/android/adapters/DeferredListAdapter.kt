package com.lasthopesoftware.bluewater.shared.android.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.namehillsoftware.handoff.promises.Promise

abstract class DeferredListAdapter<T, ViewHolder : RecyclerView.ViewHolder?>(diffCallback: DiffUtil.ItemCallback<T>)
	: ListAdapter<T, ViewHolder>(diffCallback) {

	@Volatile
	private var currentUpdate: Promise<Unit> = Promise.empty()

	@Synchronized
	fun updateListEventually(list: List<T>): Promise<Unit> {
		return currentUpdate
			.eventually({ PromisedListUpdate(list) }, { PromisedListUpdate(list) })
			.apply { currentUpdate = this }
	}

	private inner class PromisedListUpdate(list: List<T>) : Promise<Unit>(), Runnable {

		init {
			try {
				submitList(list, this)
			} catch(e: Throwable) {
				reject(e)
			}
		}

		override fun run() {
			resolve(Unit)
		}
	}
}
