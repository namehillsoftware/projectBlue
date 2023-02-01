package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.*
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

class ItemListAdapter internal constructor(
	context: Context,
	private val itemStringListProvider: ItemStringListProvider,
	private val itemListMenuEvents: IItemListMenuChangeHandler,
	private val storedItemAccess: AccessStoredItems,
	private val libraryId: LibraryId
) : DeferredListAdapter<Item, ItemListAdapter.ViewHolder>(context, ItemDiffer) {

	private val viewChangedHandler by lazy {
		ViewChangedHandler()
			.setOnAllMenusHidden(itemListMenuEvents)
			.setOnAnyMenuShown(itemListMenuEvents)
			.setOnViewChangedListener(itemListMenuEvents)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val lp = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

		val viewFlipper = NotifyOnFlipViewAnimator(parent.context)
		viewFlipper.layoutParams = lp

		viewFlipper.setViewChangedListener(viewChangedHandler)

		val inflater = LayoutInflater.from(viewFlipper.context)
		inflater.inflate(R.layout.layout_list_item, viewFlipper)
		inflater.inflate(R.layout.layout_browse_item_menu, viewFlipper)

		val childrenLayouts = viewFlipper.children.filterIsInstance<LinearLayout>().toList()

		return ViewHolder(viewFlipper, childrenLayouts[0], childrenLayouts[1])
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.update(getItem(position))
	}

	inner class ViewHolder(
		private val viewFlipper: NotifyOnFlipViewAnimator,
		private val listItemLayout: LinearLayout,
		itemMenu: LinearLayout
	): RecyclerView.ViewHolder(viewFlipper) {

		private val textView = LazyViewFinder<TextView>(listItemLayout, R.id.tvListItem)
		private val shuffleButton = LazyViewFinder<ImageButton>(itemMenu, R.id.btnShuffle)
		private val playButton = LazyViewFinder<ImageButton>(itemMenu, R.id.btnPlayAll)
		private val viewButton = LazyViewFinder<ImageButton>(itemMenu, R.id.btnViewFiles)
		private val syncButton = LazyViewFinder<ImageButton>(itemMenu, R.id.btnSyncItem)
		private var onSyncButtonLayoutChangeListener: View.OnLayoutChangeListener? = null

		fun update(item: Item) {
			viewFlipper.tryFlipToPreviousView()

			listItemLayout.setOnLongClickListener(LongClickViewAnimatorListener(viewFlipper))
			listItemLayout.setOnClickListener(ClickItemListener(libraryId, item))

			textView.findView().text = item.value
			shuffleButton.findView().setOnClickListener(
				ShuffleClickHandler(viewFlipper, itemStringListProvider, libraryId, item)
			)

			playButton.findView().setOnClickListener(
				PlayClickHandler(viewFlipper, itemStringListProvider, libraryId, item)
			)

			viewButton.findView().setOnClickListener(ViewFilesClickHandler(viewFlipper, libraryId, item))

			with (syncButton.findView()) {
				isEnabled = false
				onSyncButtonLayoutChangeListener?.also(::removeOnLayoutChangeListener)
				addOnLayoutChangeListener(SyncFilesIsVisibleHandler(
					viewFlipper,
					this,
					storedItemAccess,
					libraryId,
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
