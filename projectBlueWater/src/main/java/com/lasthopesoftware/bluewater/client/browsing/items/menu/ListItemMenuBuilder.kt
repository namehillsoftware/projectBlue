package com.lasthopesoftware.bluewater.client.browsing.items.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.PlayClickHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ShuffleClickHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.SyncFilesIsVisibleHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewFilesClickHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

class ListItemMenuBuilder(
	private val storedItemAccess: StoredItemAccess,
	private val library: Library,
	private val fileListParameterProvider: IFileListParameterProvider,
	private val fileStringListProvider: FileStringListProvider
) : AbstractListItemMenuBuilder<Item>() {
	private class ViewHolder(
		private val textViewFinder: LazyViewFinder<TextView>,
		private val shuffleButtonFinder: LazyViewFinder<ImageButton>,
		private val playButtonFinder: LazyViewFinder<ImageButton>,
		private val viewButtonFinder: LazyViewFinder<ImageButton>,
		private val syncButtonFinder: LazyViewFinder<ImageButton>
	) {
		var onSyncButtonLayoutChangeListener: View.OnLayoutChangeListener? = null
		val textView: TextView
			get() = textViewFinder.findView()
		val shuffleButton: ImageButton
			get() = shuffleButtonFinder.findView()
		val playButton: ImageButton
			get() = playButtonFinder.findView()
		val viewButton: ImageButton
			get() = viewButtonFinder.findView()
		val syncButton: ImageButton
			get() = syncButtonFinder.findView()
	}

	override fun getView(position: Int, item: Item, convertView: View?, parent: ViewGroup): View {
		var parentView = convertView as? NotifyOnFlipViewAnimator
		if (parentView == null) {
			val lp = AbsListView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			)
			parentView = NotifyOnFlipViewAnimator(parent.context)
			parentView.layoutParams = lp
			val inflater =
				parentView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			val listItemLayout =
				inflater.inflate(R.layout.layout_list_item, parentView, false) as LinearLayout
			parentView.addView(listItemLayout)
			val fileMenu = inflater.inflate(
				R.layout.layout_browse_item_menu,
				parentView,
				false
			) as LinearLayout
			parentView.addView(fileMenu)
			parentView.tag =
				ViewHolder(
					LazyViewFinder(listItemLayout, R.id.tvListItem),
					LazyViewFinder(fileMenu, R.id.btnShuffle),
					LazyViewFinder(fileMenu, R.id.btnPlayAll),
					LazyViewFinder(fileMenu, R.id.btnViewFiles),
					LazyViewFinder(fileMenu, R.id.btnSyncItem)
				)
		}
		parentView.setViewChangedListener(onViewChangedListener)
		if (parentView.displayedChild != 0) parentView.showPrevious()
		val viewHolder = parentView.tag as ViewHolder
		viewHolder.textView.text = item.value
		viewHolder.shuffleButton.setOnClickListener(
			ShuffleClickHandler(
				parentView,
				fileListParameterProvider,
				fileStringListProvider,
				item
			)
		)
		viewHolder.playButton.setOnClickListener(
			PlayClickHandler(
				parentView,
				fileListParameterProvider,
				fileStringListProvider,
				item
			)
		)
		viewHolder.viewButton.setOnClickListener(ViewFilesClickHandler(parentView, item))
		viewHolder.syncButton.isEnabled = false
		if (viewHolder.onSyncButtonLayoutChangeListener != null) viewHolder.syncButton.removeOnLayoutChangeListener(
			viewHolder.onSyncButtonLayoutChangeListener
		)
		viewHolder.onSyncButtonLayoutChangeListener = SyncFilesIsVisibleHandler(
			parentView,
			viewHolder.syncButton,
			storedItemAccess,
			library,
			item
		)
		viewHolder.syncButton.addOnLayoutChangeListener(viewHolder.onSyncButtonLayoutChangeListener)
		return parentView
	}
}
