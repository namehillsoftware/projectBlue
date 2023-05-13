package com.lasthopesoftware.bluewater.shared.android.view

import androidx.recyclerview.widget.DiffUtil

class AnyDiffer<T : Any> : DiffUtil.ItemCallback<T>() {
	override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
		oldItem == newItem

	override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = areItemsTheSame(oldItem, newItem)
}
