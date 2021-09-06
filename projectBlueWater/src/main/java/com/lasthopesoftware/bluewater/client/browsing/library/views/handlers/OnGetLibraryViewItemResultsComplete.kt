package com.lasthopesoftware.bluewater.client.browsing.library.views.handlers

import android.app.Activity
import android.view.View
import android.widget.ListView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ClickItemListener
import com.lasthopesoftware.bluewater.client.browsing.items.list.DemoableItemListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.tutorials.ManageTutorials
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class OnGetLibraryViewItemResultsComplete(
	private val activity: Activity,
	private val listView: ListView,
	private val loadingView: View,
	private val itemListMenuChangeHandler: IItemListMenuChangeHandler,
	private val fileListParameterProvider: IFileListParameterProvider,
	private val fileStringListProvider: FileStringListProvider,
	private val storedItemAccess: StoredItemAccess,
	private val library: Library,
	private val manageTutorials: ManageTutorials
) : ImmediateResponse<List<Item>, Unit> {
	override fun respond(result: List<Item>) {
		if (result.isEmpty()) return

		listView.onItemLongClickListener = LongClickViewAnimatorListener()
		listView.adapter = DemoableItemListAdapter(
			activity,
			R.id.tvStandard,
			result,
			fileListParameterProvider,
			fileStringListProvider,
			itemListMenuChangeHandler,
			storedItemAccess,
			library,
			manageTutorials
		)
		loadingView.visibility = View.INVISIBLE
		listView.visibility = View.VISIBLE
		listView.onItemClickListener = ClickItemListener(result, loadingView)
	}
}
