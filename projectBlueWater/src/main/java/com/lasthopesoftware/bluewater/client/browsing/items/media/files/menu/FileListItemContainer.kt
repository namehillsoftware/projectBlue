package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.RelativeLayout
import android.widget.TextView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

/**
 * Created by david on 4/14/15.
 */
class FileListItemContainer(parentContext: Context) {
	val textViewContainer: RelativeLayout
	private val textViewFinder: LazyViewFinder<TextView>
	val viewAnimator = NotifyOnFlipViewAnimator(parentContext)

	init {
		viewAnimator.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		val inflater = parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		textViewContainer = inflater.inflate(R.layout.layout_standard_text, viewAnimator, false) as RelativeLayout
		textViewFinder = LazyViewFinder(textViewContainer, R.id.tvStandard)
		viewAnimator.addView(textViewContainer)
	}

	fun findTextView(): TextView {
		return textViewFinder.findView()
	}
}
