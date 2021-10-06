package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.*
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

open class ItemListAdapter internal constructor(
	activity: Activity,
	private val fileListParameterProvider: IFileListParameterProvider,
	private val fileStringListProvider: FileStringListProvider,
	private val itemListMenuEvents: IItemListMenuChangeHandler,
	private val storedItemAccess: StoredItemAccess,
	private val library: Library
) : DeferredListAdapter<Item, ItemListAdapter.ViewHolder>(activity, ItemDiffer) {

	private val viewChangedHandler by lazy {
		ViewChangedHandler()
			.setOnAllMenusHidden(itemListMenuEvents)
			.setOnAnyMenuShown(itemListMenuEvents)
			.setOnViewChangedListener(itemListMenuEvents)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val lp = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		val viewAnimator = NotifyOnFlipViewAnimator(parent.context)
		viewAnimator.layoutParams = lp
		val inflater =
			viewAnimator.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val listItemLayout =
			inflater.inflate(R.layout.layout_list_item, viewAnimator, false) as LinearLayout
		viewAnimator.addView(listItemLayout)
		val itemMenu = inflater.inflate(R.layout.layout_browse_item_menu, viewAnimator, false) as LinearLayout
		viewAnimator.addView(itemMenu)
		viewAnimator.setViewChangedListener(viewChangedHandler)
		return ViewHolder(viewAnimator)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.update(getItem(position))
	}

	inner class ViewHolder(private val parentViewAnimator: NotifyOnFlipViewAnimator): RecyclerView.ViewHolder(parentViewAnimator) {

		private val textView = LazyViewFinder<TextView>(parentViewAnimator, R.id.tvListItem)
		private val shuffleButton = LazyViewFinder<ImageButton>(parentViewAnimator, R.id.btnShuffle)
		private val playButton = LazyViewFinder<ImageButton>(parentViewAnimator, R.id.btnPlay)
		private val viewButton = LazyViewFinder<ImageButton>(parentViewAnimator, R.id.btnViewFiles)
		private val syncButton = LazyViewFinder<ImageButton>(parentViewAnimator, R.id.btnSyncItem)

		var onSyncButtonLayoutChangeListener: View.OnLayoutChangeListener? = null

		fun update(item: Item) {
			if (parentViewAnimator.displayedChild != 0) parentViewAnimator.showPrevious()
			textView.findView().text = item.value
			shuffleButton.findView().setOnClickListener(
				ShuffleClickHandler(
					parentViewAnimator,
					fileListParameterProvider,
					fileStringListProvider,
					item
				)
			)

			playButton.findView().setOnClickListener(
				PlayClickHandler(
					parentViewAnimator,
					fileListParameterProvider,
					fileStringListProvider,
					item
				)
			)

			viewButton.findView().setOnClickListener(ViewFilesClickHandler(parentViewAnimator, item))

			with (syncButton.findView()) {
				isEnabled = false
				onSyncButtonLayoutChangeListener?.also(::removeOnLayoutChangeListener)
				addOnLayoutChangeListener(SyncFilesIsVisibleHandler(
					parentViewAnimator,
					this,
					storedItemAccess,
					library,
					item
				).also { onSyncButtonLayoutChangeListener = it })
			}
		}
	}

	private object ItemDiffer : DiffUtil.ItemCallback<Item>() {
		override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem.key == newItem.key

		override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem.value == newItem.value
	}
}
