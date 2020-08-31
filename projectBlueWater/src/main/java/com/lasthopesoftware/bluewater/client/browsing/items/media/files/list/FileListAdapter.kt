package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

internal class FileListAdapter(private val serviceFiles: List<ServiceFile>, private val fileListItemMenuBuilder: FileListItemMenuBuilder)
	: RecyclerView.Adapter<FileListItemMenuBuilder.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =	fileListItemMenuBuilder.newViewHolder(parent)

	override fun onBindViewHolder(holder: FileListItemMenuBuilder.ViewHolder, position: Int) =
		holder.update(PositionedFile(position, serviceFiles[position]))

	override fun getItemCount() = serviceFiles.size
}
