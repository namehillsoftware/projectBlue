package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.INowPlayingFileProvider

internal class FileListAdapter(private val serviceFiles: List<ServiceFile>, itemListMenuChangeHandler: IItemListMenuChangeHandler, nowPlayingFileProvider: INowPlayingFileProvider)
	: RecyclerView.Adapter<FileListItemMenuBuilder.ViewHolder>() {

	private val fileListItemMenuBuilder = FileListItemMenuBuilder(serviceFiles, nowPlayingFileProvider)

	init {
		fileListItemMenuBuilder.setOnViewChangedListener(
			ViewChangedHandler()
				.setOnViewChangedListener(itemListMenuChangeHandler)
				.setOnAnyMenuShown(itemListMenuChangeHandler)
				.setOnAllMenusHidden(itemListMenuChangeHandler))
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListItemMenuBuilder.ViewHolder {
		return fileListItemMenuBuilder.buildView(parent)
	}

	override fun onBindViewHolder(holder: FileListItemMenuBuilder.ViewHolder, position: Int) {
		holder.update(PositionedFile(position, serviceFiles[position]))
	}

	override fun getItemCount(): Int = serviceFiles.size
}
