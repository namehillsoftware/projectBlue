package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.list

import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.AbstractFileListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.vedsoft.futures.runnables.OneParameterAction

class NowPlayingFileListAdapter(context: Context, resource: Int, itemListMenuChangeHandler: IItemListMenuChangeHandler, serviceFiles: List<ServiceFile>, nowPlayingRepository: INowPlayingRepository) : AbstractFileListAdapter(context, resource, serviceFiles), OneParameterAction<Int> {
	private val handler = lazy { Handler(context.mainLooper) }
	private val nowPlayingFileListItemMenuBuilder: NowPlayingFileListItemMenuBuilder

	init {
		val viewChangedHandler = ViewChangedHandler()
		viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler)
		viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler)
		viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler)
		nowPlayingFileListItemMenuBuilder = NowPlayingFileListItemMenuBuilder(nowPlayingRepository)
		nowPlayingFileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler)
		nowPlayingFileListItemMenuBuilder.setOnPlaylistFileRemovedListener(this)
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		return nowPlayingFileListItemMenuBuilder.getView(position, getItem(position)!!, convertView, parent)
	}

	override fun runWith(position: Int) {
		handler.value.post { remove(getItem(position)) }
	}
}
