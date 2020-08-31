package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.menu.BuildListItemMenuViewContainers
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener

abstract class AbstractFileListItemMenuBuilder<TViewContainer : RecyclerView.ViewHolder> : BuildListItemMenuViewContainers<TViewContainer> {
	private var onViewChangedListener: OnViewChangedListener? = null

	fun setOnViewChangedListener(onViewChangedListener: OnViewChangedListener?) {
		this.onViewChangedListener = onViewChangedListener
	}

	override fun newViewHolder(parent: ViewGroup): TViewContainer {
		val fileItemMenu = FileListItemContainer(parent.context)
		val notifyOnFlipViewAnimator = fileItemMenu.viewAnimator

		val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val fileMenu = inflater.inflate(R.layout.layout_file_item_menu, parent, false) as LinearLayout

		notifyOnFlipViewAnimator.addView(fileMenu)
		notifyOnFlipViewAnimator.setViewChangedListener(onViewChangedListener)
		notifyOnFlipViewAnimator.setOnLongClickListener(LongClickViewAnimatorListener())

		return newViewHolder(fileItemMenu)
	}

	abstract fun newViewHolder(fileItemMenu: FileListItemContainer): TViewContainer
}
