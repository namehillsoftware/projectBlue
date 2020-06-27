package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.list

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileDiffer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

class NowPlayingFileListAdapter(
	itemListMenuChangeHandler: IItemListMenuChangeHandler,
	nowPlayingRepository: INowPlayingRepository)
	: ListAdapter<ServiceFile, NowPlayingFileListItemMenuBuilder.ViewHolder>(ServiceFileDiffer)/*, OneParameterAction<Int>*/ {
	private val nowPlayingFileListItemMenuBuilder = NowPlayingFileListItemMenuBuilder(nowPlayingRepository)

	init {
		val viewChangedHandler = ViewChangedHandler()
		viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler)
		viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler)
		viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler)

		nowPlayingFileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NowPlayingFileListItemMenuBuilder.ViewHolder {
		return nowPlayingFileListItemMenuBuilder.newViewHolder(parent)
	}

	override fun onBindViewHolder(holder: NowPlayingFileListItemMenuBuilder.ViewHolder, position: Int) {
		nowPlayingFileListItemMenuBuilder.setupView(holder, PositionedFile(position, getItem(position)))
	}
}
