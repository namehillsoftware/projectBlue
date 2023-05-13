package com.lasthopesoftware.bluewater.shared.android.view

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter

class ComposeRecyclerViewAdapter<T : Any>(context: Context, private val renderItem: @Composable (T) -> Unit) : DeferredListAdapter<T, ComposeRecyclerViewAdapter<T>.ViewHolder>(
	context,
	AnyDiffer()
) {

	inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
		fun updateItem(item: T) {
			composeView.setContent {
				renderItem(item)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeRecyclerViewAdapter<T>.ViewHolder =
		ViewHolder(ComposeView(parent.context))

	override fun onBindViewHolder(holder: ComposeRecyclerViewAdapter<T>.ViewHolder, position: Int) =
		holder.updateItem(getItem(position))
}
