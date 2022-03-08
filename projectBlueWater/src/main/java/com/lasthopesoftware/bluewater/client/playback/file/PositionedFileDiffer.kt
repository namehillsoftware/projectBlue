package com.lasthopesoftware.bluewater.client.playback.file

import androidx.recyclerview.widget.DiffUtil

object PositionedFileDiffer : DiffUtil.ItemCallback<PositionedFile>() {
	override fun areItemsTheSame(oldItem: PositionedFile, newItem: PositionedFile): Boolean = oldItem == newItem

	override fun areContentsTheSame(oldItem: PositionedFile, newItem: PositionedFile): Boolean = true
}
