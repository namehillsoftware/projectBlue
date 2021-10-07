package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider.Companion.provide
import com.lasthopesoftware.bluewater.client.browsing.items.list.ClickItemListener
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListActivity.Companion.startFileListActivity
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import org.joda.time.Duration
import org.slf4j.LoggerFactory

class ClickItemListener(private val item: Item, private val recyclerView: RecyclerView, private val loadingView: View) :
	View.OnClickListener {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(ClickItemListener::class.java) }
	}

	override fun onClick(view: View?) {
		val context = view?.context ?: return

        recyclerView.visibility = ViewUtils.getVisibility(false)
        loadingView.visibility = ViewUtils.getVisibility(true)
        getInstance(context).promiseSessionConnection()
            .eventually { c -> c?.let { provide(c, item.key).keepPromise(emptyList()) } }
			.then({ items ->
                    if (items.isNotEmpty()) {
                        val itemListIntent = Intent(context, ItemListActivity::class.java)
                        itemListIntent.putExtra(ItemListActivity.KEY, item.key)
                        itemListIntent.putExtra(ItemListActivity.VALUE, item.value)
                        context.startActivity(itemListIntent)
                    } else {
						startFileListActivity(context, item)
					}
                },
                { e ->
                    logger.error(
                        "An error occurred getting nested items for item " + item.key,
                        e
                    )
                })
            .eventually {
                LoopedInPromise<Any?>(
                    {
                        recyclerView.visibility = ViewUtils.getVisibility(true)
                        loadingView.visibility = ViewUtils.getVisibility(false)
                    },
					context,
					Duration.standardSeconds(1))
            }
    }
}
