package com.lasthopesoftware.bluewater.shared.android.adapters

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.promises.extensions.LoopedInPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise

abstract class DeferredListAdapter<T, ViewHolder : RecyclerView.ViewHolder?>(
	context: Context,
	diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, ViewHolder>(diffCallback) {

	private val handler by lazy { Handler(context.mainLooper) }

	@Volatile
	private var currentUpdate: Promise<Unit> = Unit.toPromise()

	@Synchronized
	open fun updateListEventually(list: List<T>): Promise<Unit> =
		currentUpdate
			.inevitably { PromisedListUpdate(list) }
			.apply { currentUpdate = this }

	private inner class PromisedListUpdate(list: List<T>) : LoopedInPromise<Unit>(
		MessengerOperator<Unit> {
			try {
				submitList(list) { it.sendResolution(Unit) }
			} catch (e: Throwable) {
				it.sendRejection(e)
			}
		},
		handler)
}
