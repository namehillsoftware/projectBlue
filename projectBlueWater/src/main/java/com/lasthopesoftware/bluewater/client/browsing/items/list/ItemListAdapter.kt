package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess

open class ItemListAdapter internal constructor(
	activity: Activity,
	resource: Int,
	items: List<Item>,
	fileListParameterProvider: IFileListParameterProvider,
	fileStringListProvider: FileStringListProvider,
	itemListMenuEvents: IItemListMenuChangeHandler,
	storedItemAccess: StoredItemAccess,
	library: Library
) : ArrayAdapter<Item>(activity, resource, items) {
	private val listItemMenuBuilder: ListItemMenuBuilder

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		return listItemMenuBuilder.getView(position, getItem(position)!!, convertView, parent)
	}

	init {
		val viewChangedHandler = ViewChangedHandler()
		viewChangedHandler.setOnAllMenusHidden(itemListMenuEvents)
		viewChangedHandler.setOnAnyMenuShown(itemListMenuEvents)
		viewChangedHandler.setOnViewChangedListener(itemListMenuEvents)
		listItemMenuBuilder =
			ListItemMenuBuilder(storedItemAccess, library, fileListParameterProvider, fileStringListProvider)
		listItemMenuBuilder.onViewChangedListener = viewChangedHandler
	}
}
