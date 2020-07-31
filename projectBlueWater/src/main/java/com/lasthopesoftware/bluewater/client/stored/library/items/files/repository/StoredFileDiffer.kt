package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import androidx.recyclerview.widget.DiffUtil

object StoredFileDiffer: DiffUtil.ItemCallback<StoredFile>() {
	override fun areItemsTheSame(oldItem: StoredFile, newItem: StoredFile): Boolean = oldItem.id == newItem.id

	override fun areContentsTheSame(oldItem: StoredFile, newItem: StoredFile): Boolean = oldItem.id == newItem.id
}
