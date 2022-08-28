package com.lasthopesoftware.bluewater.client.browsing.files.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.RelativeLayout
import android.widget.TextView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

class FileListItemContainer(parentContext: Context) {
	private val textViewContainer: RelativeLayout
	private val textViewFinder: LazyViewFinder<TextView>

	val viewAnimator = NotifyOnFlipViewAnimator(parentContext)
	val textView
		get() = textViewFinder.findView()

	init {
		viewAnimator.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		val inflater = parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		textViewContainer = inflater.inflate(R.layout.layout_standard_text, viewAnimator, false) as RelativeLayout
		textViewFinder = LazyViewFinder(textViewContainer, R.id.tvStandard)
		viewAnimator.addView(textViewContainer)
	}
}
