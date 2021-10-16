package com.lasthopesoftware.bluewater.client.browsing.library.views.handlers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class OnGetLibraryViewItemResultsComplete(
	private val adapter: DeferredListAdapter<Item, *>,
	private val listView: RecyclerView,
	private val loadingView: View
) : ImmediateResponse<List<Item>, Unit> {
	override fun respond(result: List<Item>) {
		if (result.isEmpty()) return

		adapter.updateListEventually(result)
		loadingView.visibility = View.INVISIBLE
		listView.visibility = View.VISIBLE
	}
}
