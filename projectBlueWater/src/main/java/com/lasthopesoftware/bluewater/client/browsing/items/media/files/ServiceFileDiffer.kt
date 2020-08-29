package com.lasthopesoftware.bluewater.client.browsing.items.media.files

import androidx.recyclerview.widget.DiffUtil

object ServiceFileDiffer : DiffUtil.ItemCallback<ServiceFile>() {
	override fun areItemsTheSame(oldItem: ServiceFile, newItem: ServiceFile): Boolean = oldItem == newItem

	override fun areContentsTheSame(oldItem: ServiceFile, newItem: ServiceFile): Boolean =
		areItemsTheSame(oldItem, newItem)
}
